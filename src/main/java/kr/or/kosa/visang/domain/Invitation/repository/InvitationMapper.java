package kr.or.kosa.visang.domain.Invitation.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.kosa.visang.domain.Invitation.model.Invitation;

@Mapper
public interface InvitationMapper {

	void insertInvitation(Invitation inv);

	void deleteByContractId(@Param("contractId") Long contractId);

	int updateEmailSent(@Param("invitationId") Long invitationId, @Param("emailSent") String emailSent);
}
