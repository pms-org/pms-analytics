// package com.pms.analytics.service;

// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.stereotype.Service;

// @Service
// public class AdvisoryLockService {

//     private final JdbcTemplate jdbcTemplate;

//     public AdvisoryLockService(JdbcTemplate jdbcTemplate)
//     {
//         this.jdbcTemplate = jdbcTemplate;
//     }

//     //Try to aquire lock on portfolio id
//     public boolean acquireLock(String portfolioId)
//     {
//         return Boolean.TRUE.equals(
//             jdbcTemplate.queryForObject(
//                 "SELECT pg_try_advisory_lock(hashtext(?))",
//                 Boolean.class,
//                 portfolioId
//             )
//         );
//     }

//     //Release lock on portfolio id
//     public void releaseLock(String portfolioId)
//     {
//         jdbcTemplate.queryForObject(
//             "SELECT pg_advisory_unlock(hashText(?))",
//             Boolean.class,
//             portfolioId
//         );
//     }
// }
