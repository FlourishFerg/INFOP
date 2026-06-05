// package com.infopouch.api.persistence.entity;
//
// import jakarta.persistence.*;
// import javax.management.relation.Role;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;
//
//// User.java
// @Entity
// @Table(name = "users")
// @Getter
// @Setter
// @NoArgsConstructor
// public class User {
//  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
//  private Long id;
//
//  @Column(unique = true, nullable = false)
//  private String email;
//
//  @Column(nullable = false)
//  private String password;
//
//  @Enumerated(EnumType.STRING)
//  private Role role;
//
//  private boolean enabled = false; // For email verification later
// }
