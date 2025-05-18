package kr.or.kosa.visang.domain.Contract.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Contract {
	private Long contractId;
	private LocalDateTime contractTime;
	private LocalDateTime createdAt;
	private Long clientId;
	private Long agentId;
	private Long contractTemplateId;
	private String status;
	private String memo;
}
