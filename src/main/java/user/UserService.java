package user;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.safetag.DataNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SiteUser create(String username, String email, String password, String gender, String phoneNum) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setGender(gender);
        user.setPhoneNum(phoneNum);
        this.userRepository.save(user);
        return user;
    }

    public SiteUser getUser(String username) {
        Optional<SiteUser> siteUser = this.userRepository.findByUsername(username);
        if (siteUser.isPresent()) {
            return siteUser.get();
        } else {
            throw new DataNotFoundException("siteuser not found");
        }
    }

    public SiteUser updateUser(String username, String email, String password, String gender, String phoneNum, String carNumber, String apartmentInfo) {
        SiteUser user = getUser(username);
        user.setEmail(email);
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setGender(gender);
        user.setPhoneNum(phoneNum);
        user.setCarNumber(carNumber);
        user.setApartmentInfo(apartmentInfo);
        this.userRepository.save(user);
        return user;
    }
}
