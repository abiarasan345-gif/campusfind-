package com.example.campusfind.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.campusfind.entity.FoundItem;
import com.example.campusfind.entity.FoundStatus;

public interface FoundItemRepository extends JpaRepository<FoundItem, Long> {

    long countByStatus(FoundStatus status);

    @Query("""
            select f from FoundItem f
            where (:status is null or f.status = :status)
              and (:categoryId is null or f.category.id = :categoryId)
              and (:location = '' or lower(f.location) like lower(concat('%', :location, '%')))
              and (:keyword = '' or lower(f.itemName) like lower(concat('%', :keyword, '%'))
                   or lower(f.description) like lower(concat('%', :keyword, '%')))
            order by f.createdAt desc
            """)
    List<FoundItem> search(
            @Param("status") FoundStatus status,
            @Param("categoryId") Long categoryId,
            @Param("location") String location,
            @Param("keyword") String keyword);
}
