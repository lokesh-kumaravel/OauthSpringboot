package com.lokesh.OauthSpringboot;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;



@Repository
public interface UserRepo extends MongoRepository<User,String>
{
    User findByUsername(String username);
    User getUserById(String userId);
    User findByEmail(String email);
    // User findByMailId(String email);
}