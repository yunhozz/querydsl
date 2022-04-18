package study.querydsl.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/*
Gradle -> Tasks -> other -> compileQuerydsl 실행
generated/querydsl/study/querydsl/entity/QHello.java 생성
 */
@Entity
@Getter @Setter
public class Hello {

    @Id
    @GeneratedValue
    private Long id;
}
