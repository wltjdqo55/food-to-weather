package suggest.taste_the_weather.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import suggest.taste_the_weather.config.KakaoProperties;
import suggest.taste_the_weather.model.dto.UserDTO;
import suggest.taste_the_weather.model.entity.User;
import suggest.taste_the_weather.model.vo.UserVO;
import suggest.taste_the_weather.repository.jpa.AccountRepository;
import suggest.taste_the_weather.repository.querydsl.AccountQueryDSL;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

  private final AccountRepository accountRepository;
  private final AccountQueryDSL accountQueryDSL;
  private final KakaoProperties kakaoProperties;

  String client_id = "a8eda776e6a96137d25561b1e5fe581a"; // 앱 키
  String response_type = "code";  // code로 고정
  String charset = "UTF-8";   // 인코딩 설정

  public String getKakaoAuthorizeUrl () throws UnsupportedEncodingException {
    // URL 인코딩
    String encodedRedirectUri = URLEncoder.encode(kakaoProperties.getRedirectUri(), charset);
    String encodedClientId = URLEncoder.encode(client_id, charset);

    String apiUrl = "https://kauth.kakao.com/oauth/authorize?response_type=" + response_type + "&client_id=" + encodedClientId
        + "&redirect_uri=" + encodedRedirectUri;
    
    return apiUrl; // 생성된 uri 반환
  }

  public String getAccessToken(String code) throws IOException {
    String accessToken = "";
    String refreshToken = "";
    String reqUrl = "https://kauth.kakao.com/oauth/token";

    try {
      URL url = new URL(reqUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();

      // 필수 헤더 세팅
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
      conn.setDoOutput(true);

      // POST 데이터 작성
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
      StringBuilder sb = new StringBuilder();
      sb.append("grant_type=authorization_code");
      sb.append("&client_id=").append(client_id);
      sb.append("&redirect_uri=").append(kakaoProperties.getRedirectUri());
      sb.append("&code=").append(code);

      bw.write(sb.toString());
      bw.flush();

      // 응답 코드 확인
      int responseCode = conn.getResponseCode();
      System.out.println("Response Code: " + responseCode);

      // 응답 읽기
      BufferedReader br;
      if (responseCode >= 200 && responseCode < 300) {
        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      } else {
        br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
      }

      String line;
      StringBuilder responseSb = new StringBuilder();
      while ((line = br.readLine()) != null) {
        responseSb.append(line);
      }
      String result = responseSb.toString();
      System.out.println("Response Body: " + result);

      // JSON 파싱
      JSONObject jsonObject = new JSONObject(result);
      accessToken = jsonObject.getString("access_token");
      refreshToken = jsonObject.getString("refresh_token");

      System.out.println("Access Token: " + accessToken);
      System.out.println("Refresh Token: " + refreshToken);

      br.close();
      bw.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
    return accessToken;
  }

  public Map<String, Object> getUserInfo (String accessToken) {
    HashMap<String, Object> userInfo = new HashMap<>();
    String reqUrl = "https://kapi.kakao.com/v2/user/me";

    try {
      URL url = new URL(reqUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();

      // 요청 메서드 및 헤더 설정
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Authorization", "Bearer " + accessToken);
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

      int responseCode = conn.getResponseCode();
      System.out.println("Response Code: " + responseCode);

      BufferedReader br;
      if (responseCode >= 200 && responseCode < 300) {
        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      } else {
        br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
      }

      String line;
      StringBuilder responseSb = new StringBuilder();
      while ((line = br.readLine()) != null) {
        responseSb.append(line);
      }
      String result = responseSb.toString();
      System.out.println("Response Body: " + result);

      // JSON 파싱하여 필요한 정보 추출
      JSONObject jsonObject = new JSONObject(result);
      JSONObject kakaoAccount = jsonObject.getJSONObject("kakao_account");
      JSONObject profile = kakaoAccount.getJSONObject("profile");

      userInfo.put("id", jsonObject.getLong("id"));
      userInfo.put("nickname", profile.getString("nickname"));
      System.out.println("userInfo => " + userInfo);

      br.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
    return userInfo;
  }

  public UserDTO setUserInfo (String userId, String name) {
    System.out.println(userId + " " + name);

    UserVO userVO = new UserVO(userId, name, "kakao");

    if(accountRepository.findOneByUserId(userId).isEmpty()) {   //가입된 정보가 없을 시 계정 새로 등록
      accountRepository.save(new User(userVO));
    }

    return new UserDTO(accountQueryDSL.findUserInfo(userId));
  }
}
