package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection //QMemberDto 생성, 하지만 querydsl 에 의존하게 되어 순수성이 약해짐
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
