package com.pms.analytics.dao;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pms.analytics.dao.entity.DltOutbox;

@Repository
public interface DltOutboxDao extends JpaRepository<DltOutbox, UUID> {

}
