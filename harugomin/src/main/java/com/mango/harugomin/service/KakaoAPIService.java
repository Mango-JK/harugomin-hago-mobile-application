package com.mango.harugomin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.harugomin.domain.entity.User;
import com.mango.harugomin.dto.UserRequestDto;
import com.mango.harugomin.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

@Slf4j
@RequiredArgsConstructor
@Service
public class KakaoAPIService {

    private final UserService userService;
    private final JwtService jwtService;

    private final String requestURL = "https://kapi.kakao.com/v2/user/me";
    private final String AUTH_HOST = "https://kauth.kakao.com";

    public ResponseEntity<String> getAccessToken(String code) {
        final String tokenRequestUrl = AUTH_HOST + "/oauth/token";

        String CLIENT_ID = "7a888c52e90c278c82e7da483c93375f";
        String REDIRECT_URI = "http://52.78.127.67:8080/api/v1/users/login/kakao";
//        String REDIRECT_URI = "http://localhost:8080/api/v1/users/login/kakao";

        HttpsURLConnection conn = null;
        OutputStreamWriter writer = null;
        BufferedReader reader = null;
        InputStreamReader isr = null;
        final StringBuffer buffer = new StringBuffer();

        try {
            final String params = String.format("grant_type=authorization_code&client_id=%s&redirect_uri=%s&code=%s",
                    CLIENT_ID, REDIRECT_URI, code);

            final URL url = new URL(tokenRequestUrl);

            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(params);
            writer.flush();

            int responseCode = conn.getResponseCode();

            isr = new InputStreamReader(conn.getInputStream());
            reader = new BufferedReader(isr);

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignore) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignore) {
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception ignore) {
                }
            }
        }
        return new ResponseEntity<String>(buffer.toString(), HttpStatus.OK);
    }

    public JsonNode getKaKaoUserInfo(String access_Token) {

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpPost post = new HttpPost(requestURL);

        post.addHeader("Authorization", "Bearer " + access_Token);
        JsonNode returnNode = null;

        HttpResponse response;
        try {
            response = client.execute(post);
            ObjectMapper mapper = new ObjectMapper();
            returnNode = mapper.readTree(response.getEntity().getContent());
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnNode;
    }

    @Transactional
    public String redirectToken(JsonNode json) {
        long id = json.get("id").asLong();
        String ageRange = "0";
        String nickname = "kakaoUser";

        int random = (int) Math.round(Math.random() * 4) + 1;
        String image = "https://hago-storage-bucket.s3.ap-northeast-2.amazonaws.com/default0" + random + ".jpg";
        User user = null;
        if (userService.findById(id).isEmpty()) {
            User newUser = User.builder()
                    .userId(id)
                    .nickname(nickname)
                    .profileImage(image)
                    .ageRange(Integer.parseInt(ageRange))
                    .build();
            user = userService.saveUser(newUser);
        } else {
            user = userService.findById(id).get();
        }
        UserRequestDto userRequestDto = new UserRequestDto(user);
        String jwt = jwtService.create("user", userRequestDto, "user");

        return jwt;
    }
}