package com.mango.harugomin.service;

import com.mango.harugomin.domain.entity.UserHashtag;
import com.mango.harugomin.domain.repository.UserHashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserHashtagService {
    private final UserHashtagRepository userHashtagRepository;

    public long deleteByUser(Long userId) {
        return userHashtagRepository.deleteByUserUserId(userId);
    }

    public void save(UserHashtag newUserHashtag) {
        userHashtagRepository.save(newUserHashtag);
    }
}
