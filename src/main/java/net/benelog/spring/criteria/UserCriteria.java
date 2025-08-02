package net.benelog.spring.criteria;

public class UserCriteria {
	UserGrade grade;

	public UserCriteria(UserGrade grade) {
		this.grade = grade;
	}

	public UserGrade getGrade() {
		return grade;
	}
}
