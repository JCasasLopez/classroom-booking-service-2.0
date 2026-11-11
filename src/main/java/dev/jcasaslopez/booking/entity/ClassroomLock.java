package dev.jcasaslopez.booking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="classroom_locks")
public class ClassroomLock {

	@Id
	private int idClassroom;

	protected ClassroomLock() {}   

	public ClassroomLock(int idClassroom) {
		this.idClassroom = idClassroom;
	}

	public int getIdClassroom() {
		return idClassroom;
	}
}