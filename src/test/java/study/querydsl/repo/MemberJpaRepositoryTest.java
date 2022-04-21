package study.querydsl.repo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired EntityManager em;
    @Autowired MemberJpaRepository memberJpaRepository;

    @Test
    void basicTest() {
        //given
        Member member = new Member("member1", 10);

        //when
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        List<Member> findMembers = memberJpaRepository.findAll_Querydsl();
        List<Member> result = memberJpaRepository.findByUsername_Querydsl("member1");

        //then
        assertThat(findMember).isEqualTo(member);
        assertThat(findMembers).containsExactly(member);
        assertThat(result).containsExactly(member);
    }

    @Test
    void searchTest() {
        //given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member(teamA, "member1", 10);
        Member member2 = new Member(teamA, "member2", 20);
        Member member3 = new Member(teamB, "member3", 30);
        Member member4 = new Member(teamB, "member4", 40);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(20);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        //when
        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);

        //then
        assertThat(result).extracting("username").containsExactly("member3", "member4");
    }
}