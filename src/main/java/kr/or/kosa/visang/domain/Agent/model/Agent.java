package kr.or.kosa.visang.domain.Agent.model;

import java.sql.Date;
import lombok.Data;

@Data
public class Agent {
	private Long agentId;
	private String name;
	private String email;
	private String password;
	private String phoneNumber;
	private String address;
	private String role;
	private Date createAt;
}
