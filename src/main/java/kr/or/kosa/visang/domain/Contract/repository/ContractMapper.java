package kr.or.kosa.visang.domain.Contract.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.kosa.visang.domain.Contract.model.Contract;
import kr.or.kosa.visang.domain.Contract.model.Schedule;

@Mapper
public interface ContractMapper {

	void insertSchedule(Contract c);

	List<Schedule> selectSchedulesByAgent(@Param("agentId") Long agentId);

	int countByClientAndTime(@Param("clientId") Long clientId, @Param("contractTime") LocalDateTime contractTime);

	int countByAgentAndTime(@Param("agentId") Long agentId, @Param("contractTime") LocalDateTime contractTime);

	//  excludeContractId : 자기 자신 제외
	int countByClientAndTimeExcept(@Param("clientId") Long clientId, @Param("contractTime") LocalDateTime contractTime,
			@Param("excludeContractId") Long excludeContractId);

	int countByAgentAndTimeExcept(@Param("agentId") Long agentId, @Param("contractTime") LocalDateTime contractTime,
			@Param("excludeContractId") Long excludeContractId);

	void updateSchedule(Schedule dto);

	int deleteSchedule(@Param("contractId") Long contractId);

	List<Schedule> findTodayContracts(Map<String, Object> params);

	List<Schedule> selectSchedulesByAgentAndDateRange(Map<String, Object> param);

}
