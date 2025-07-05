package doodle.qa.com.svccalendarqa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * User entity representing a user in the system. This is a local copy of user data consumed from
 * Kafka messages.
 */
@Entity
@Table(name = "users")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id private UUID id;

  @NotBlank(message = "User name is required")
  @Column(nullable = false)
  private String name;

  @Email(message = "Email should be valid")
  @NotBlank(message = "Email is required")
  @Column(nullable = false, unique = true)
  private String email;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User)) return false;
    User user = (User) o;
    return id != null && id.equals(user.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "User{" + "id=" + id + ", name='" + name + '\'' + ", email='" + email + '\'' + '}';
  }
}
