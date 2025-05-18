package kr.or.kosa.visang.domain.Agent.service;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.spring6.ISpringTemplateEngine;

import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import kr.or.kosa.visang.domain.Agent.controller.AgentController;
import kr.or.kosa.visang.domain.Client.model.Client;
import kr.or.kosa.visang.domain.Client.repository.ClientMapper;
import kr.or.kosa.visang.domain.Contract.model.Contract;
import kr.or.kosa.visang.domain.Contract.model.Schedule;
import kr.or.kosa.visang.domain.Contract.repository.ContractMapper;
import kr.or.kosa.visang.domain.Invitation.model.Invitation;
import kr.or.kosa.visang.domain.Invitation.repository.InvitationMapper;

@Service
public class AgentService {
	private static final Logger log = LoggerFactory.getLogger(AgentController.class);
	private final ISpringTemplateEngine templateEngine;

	private final ContractMapper contractMapper;
	private final InvitationMapper invitationMapper;
	private final ClientMapper clientMapper;
	private final JavaMailSender mailSender;

	public AgentService(ContractMapper contractMapper, InvitationMapper invitationMapper, ClientMapper clientMapper,
			JavaMailSender mailSender, ISpringTemplateEngine templateEngine) {
		this.contractMapper = contractMapper;
		this.invitationMapper = invitationMapper;
		this.clientMapper = clientMapper;
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
	}

	public Client findByEmail(String email) {
		return clientMapper.findByEmail(email);
	}

	@Transactional
	public String addSchedule(Schedule dto) throws MessagingException {
		LocalDateTime time = dto.getContractTime();

		// 고객 존재 확인
		if (dto.getClientId() == null) {
			Client client = clientMapper.findByEmail(dto.getEmail());
			if (client == null) {
				throw new IllegalArgumentException("해당 이메일의 고객이 없습니다.");
			}
			dto.setClientId(client.getClientId());
		}

		int agentExisting = contractMapper.countByAgentAndTime(dto.getAgentId(), time);
		if (agentExisting > 0) {
			throw new IllegalArgumentException(String.format("이미 %s에 다른 상담 일정이 있습니다.",
					time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
		}

		int existing = contractMapper.countByClientAndTime(dto.getClientId(), time);
		if (existing > 0) {
			throw new IllegalArgumentException(String.format("이미 %s에 예약된 일정이 있습니다.",
					time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
		}

		// CONTRACT 삽입
		Contract c = new Contract();
		c.setClientId(dto.getClientId());
		c.setAgentId(dto.getAgentId());
		c.setContractTime(dto.getContractTime());
		c.setMemo(dto.getMemo());
		c.setCreatedAt(LocalDateTime.now());
		contractMapper.insertSchedule(c);

		// 초대코드 생성
		String timePart = dto.getContractTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
		String randPart = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8).toUpperCase();
		String code = timePart + "-" + randPart;

		// INVITATION 삽입
		Invitation inv = new Invitation();
		inv.setContractId(c.getContractId());
		inv.setInvitationCode(code);
		inv.setCreatedAt(LocalDateTime.now());
		inv.setExpiredTime(dto.getContractTime().plusHours(1));
		inv.setEmailSent("N");
		invitationMapper.insertInvitation(inv);

		// 메일 발송
		MimeMessage mime = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

		Context ctx = new Context();
		ctx.setVariable("clientName", dto.getClientName());
		ctx.setVariable("reserveTime", dto.getContractTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
		ctx.setVariable("code", code);

		String html = templateEngine.process("invitation/email", ctx);

		helper.setFrom(""); // 보낼 이메일 넣어서 사용
		helper.setTo(dto.getEmail());
		helper.setSubject("상담 초대 코드 안내");
		helper.setText(html, true);

		mailSender.send(mime);

		// 발송여부 업데이트 "Y" 보냄
		inv.setEmailSent("Y");
		invitationMapper.updateEmailSent(inv.getInvitationId(), inv.getEmailSent());

		return code;
	}

	@Transactional(readOnly = true)
	public List<Schedule> getSchedules(Long agentId) {
		return contractMapper.selectSchedulesByAgent(agentId);
	}

	public boolean isScheduleExists(Long clientId, String contractTime) {
		LocalDateTime ct = LocalDateTime.parse(contractTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

		int cnt = contractMapper.countByClientAndTime(clientId, ct);
		return cnt > 0;
	}

	public boolean isAgentScheduleExists(Long agentId, String contractTime) {
		LocalDateTime ct = LocalDateTime.parse(contractTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		return contractMapper.countByAgentAndTime(agentId, ct) > 0;
	}

	@Transactional
	public void updateSchedule(Schedule dto) {
		LocalDateTime time = dto.getContractTime();
		Long cid = dto.getContractId();

		// 고객 일정 충돌 검사
		if (contractMapper.countByClientAndTimeExcept(dto.getClientId(), time, cid) > 0) {
			throw new IllegalArgumentException("이미 이 시간에 해당 고객 일정이 있습니다.");
		}
		// 상담사 일정 충돌 검사
		if (contractMapper.countByAgentAndTimeExcept(dto.getAgentId(), time, cid) > 0) {
			throw new IllegalArgumentException("이미 이 시간에 다른 상담 일정이 있습니다.");
		}

		contractMapper.updateSchedule(dto);
	}

	@Transactional
	public void deleteSchedule(Long contractId) {

		invitationMapper.deleteByContractId(contractId);

		int deleted = contractMapper.deleteSchedule(contractId);

		if (deleted == 0) {
			throw new IllegalArgumentException("삭제할 일정이 없습니다. ID=" + contractId);
		}
	}

	@Transactional(readOnly = true)
	public List<Schedule> getTodayContracts(Long agentId, String date) {
		Map<String, Object> params = new HashMap<>();
		params.put("agentId", agentId);
		params.put("date", date);
		return contractMapper.findTodayContracts(params);
	}

}
