package com.example.campusfind.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.campusfind.entity.LostReport;
import com.example.campusfind.entity.LostStatus;

public interface LostReportRepository extends JpaRepository<LostReport, Long> {

    long countByStatus(LostStatus status);

    Optional<LostReport> findByMatchedFoundItemId(Long foundItemId);

    @Query("""
            select l from LostReport l
            where (:status is null or l.status = :status)
              and (:categoryId is null or l.category.id = :categoryId)
              and (:location = '' or lower(l.location) like lower(concat('%', :location, '%')))
              and (:keyword = '' or lower(l.itemName) like lower(concat('%', :keyword, '%'))
                   or lower(l.description) like lower(concat('%', :keyword, '%')))
            order by l.createdAt desc
            """)
    List<LostReport> search(
            @Param("status") LostStatus status,
            @Param("categoryId") Long categoryId,
            @Param("location") String location,
            @Param("keyword") String keyword);
}
