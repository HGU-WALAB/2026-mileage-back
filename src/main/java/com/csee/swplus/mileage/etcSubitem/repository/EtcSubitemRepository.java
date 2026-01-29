package com.csee.swplus.mileage.etcSubitem.repository;

import com.csee.swplus.mileage.etcSubitem.domain.EtcSubitem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EtcSubitemRepository extends JpaRepository<EtcSubitem, Integer> {
}