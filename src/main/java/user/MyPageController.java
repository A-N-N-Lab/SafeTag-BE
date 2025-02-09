package user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.security.Principal;

@RequiredArgsConstructor
@Controller
@RequestMapping("/mypage")
public class MyPageController {

    private final UserService userService;

    @GetMapping("/edit")
    public String myPageForm(MyPageUpdateForm myPageUpdateForm, Principal principal) {
        SiteUser user = userService.getUser(principal.getName());
        myPageUpdateForm.setUsername(user.getUsername());
        myPageUpdateForm.setEmail(user.getEmail());
        myPageUpdateForm.setGender(user.getGender());
        myPageUpdateForm.setPhoneNum(user.getPhoneNum());
        myPageUpdateForm.setCarNumber(user.getCarNumber());
        myPageUpdateForm.setApartmentInfo(user.getApartmentInfo());
        return "mypage_edit";
    }
    
    @GetMapping("")
    public String myPageView(Model model, Principal principal) {
        SiteUser user = userService.getUser(principal.getName());
        model.addAttribute("user", user);
        return "mypage";
    }


    @PostMapping("/edit")
    public String updateMyPage(@Valid MyPageUpdateForm myPageUpdateForm, BindingResult bindingResult, Principal principal) {
        if (bindingResult.hasErrors()) {
            return "mypage_edit";
        }

        try {
            userService.updateUser(
                principal.getName(),
                myPageUpdateForm.getEmail(),
                myPageUpdateForm.getPassword(),
                myPageUpdateForm.getGender(),
                myPageUpdateForm.getPhoneNum(),
                myPageUpdateForm.getCarNumber(),
                myPageUpdateForm.getApartmentInfo()
            );
        } catch (Exception e) {
            bindingResult.reject("updateFailed", e.getMessage());
            return "mypage_edit";
        }

        return "redirect:/mypage";
    }
}
