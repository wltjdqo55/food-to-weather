package suggest.emotion_web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import suggest.emotion_web.model.dto.UserDTO;
import suggest.emotion_web.model.entity.User;
import suggest.emotion_web.model.vo.UserVO;
import suggest.emotion_web.repository.jpa.AccountRepository;
import suggest.emotion_web.repository.querydsl.AccountQueryDSL;

@Service
@RequiredArgsConstructor
public class AccountService {

  private final AccountRepository accountRepository;
  private final AccountQueryDSL accountQueryDSL;

  public boolean idCheck (String userId) {
    return accountRepository.findOneByUserId(userId).isEmpty();
  }

  @Transactional
  public UserDTO userAdd (UserVO userVO) {
    return new UserDTO(accountRepository.save(new User(userVO)));
  }

  public UserDTO loginCheck (UserVO userVO) {
    User user = accountQueryDSL.loginCheck(userVO);
    if(user==null) {
      return null;
    }
    return new UserDTO(user);
  }

}
