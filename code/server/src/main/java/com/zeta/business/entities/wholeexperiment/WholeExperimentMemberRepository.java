package com.zeta.business.entities.wholeexperiment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WholeExperimentMemberRepository extends JpaRepository<WholeExperimentMember, Long> {
  List<WholeExperimentMember> findByExperimentIdOrderBySequenceNoAsc(Long experimentId);
}
