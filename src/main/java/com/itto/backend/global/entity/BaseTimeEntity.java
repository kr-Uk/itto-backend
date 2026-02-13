package com.itto.backend.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * [생성 시각 자동 기록 - 모든 엔티티의 최상위 부모 클래스]
 *
 * 엔티티(Entity)란?
 * - 데이터베이스 테이블과 1:1로 대응되는 자바 클래스입니다.
 * - 예: User 엔티티 → users 테이블, Photo 엔티티 → photos 테이블
 *
 * 이 클래스의 역할:
 * - 데이터가 처음 생성된 시각(created_at)을 자동으로 기록합니다.
 * - 다른 엔티티들이 이 클래스를 상속하면, 별도 코드 없이 생성 시각이 자동으로 저장됩니다.
 *
 * abstract(추상) 클래스:
 * - 직접 객체를 만들 수 없고, 다른 클래스가 상속해서 사용하도록 만든 "틀" 클래스입니다.
 */
@MappedSuperclass                                 // JPA: 이 클래스의 필드를 상속받는 자식 엔티티의 테이블 컬럼으로 포함시킴 (이 클래스 자체는 테이블이 아님)
@EntityListeners(AuditingEntityListener.class)    // JPA: 데이터 생성/수정 시 자동으로 시각을 기록해주는 리스너를 연결
@Getter                                           // Lombok: getCreatedAt() 메서드를 자동 생성
public abstract class BaseTimeEntity {

    @CreatedDate                                                // Spring Data: 데이터가 처음 저장될 때 현재 시각을 자동으로 넣어줌
    @Column(name = "created_at", nullable = false, updatable = false)  // DB 컬럼명: created_at, 필수값, 수정 불가
    private LocalDateTime createdAt;  // 데이터가 생성된 시각 (예: 2025-01-15T14:30:00)
}
