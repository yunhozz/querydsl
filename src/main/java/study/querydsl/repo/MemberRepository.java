package study.querydsl.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
}
