package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    private String username;
    private int age;

    public Member(String username) {
        this(username, 0);
    }

    public Member(String username, int age) {
        this(null, username, age);
    }

    public Member(Team team, String username, int age) {
        setTeam(team);
        this.username = username;
        this.age = age;
    }

    //양방향 연관관계 생성
    private void setTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
