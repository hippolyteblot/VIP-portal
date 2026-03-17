package fr.insalyon.creatis.vip.core.server.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.models.SignUpForm;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.AuthenticationBusiness;
import fr.insalyon.creatis.vip.core.server.business.UserBusiness;
import fr.insalyon.creatis.vip.core.server.model.PrecisePage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserBusiness userBusiness;
    private final AuthenticationBusiness authenticationBusiness;

    @Autowired
    public UserController(UserBusiness userBusiness, AuthenticationBusiness authenticationBusiness) {
        this.userBusiness = userBusiness;
        this.authenticationBusiness = authenticationBusiness;
    }

    @GetMapping
    public PrecisePage<User> list(@RequestParam(defaultValue = "0") @PositiveOrZero int offset,
            @RequestParam(defaultValue = "10") @Positive @Max(value = 50) int quantity) throws VipException {
        return userBusiness.getAll(offset, quantity);
    }

    @GetMapping(value = "{id}")
    public User get(@PathVariable String id) throws VipException {
        User user = userBusiness.get(id);

        if (user == null) {
            throw new VipException(DefaultError.NOT_FOUND, id);
        } else {
            return user;
        }
    }

    @DeleteMapping(value = "{id}")
    public void remove(@PathVariable String id) throws VipException {
        User user = get(id);

        if (user != null) {
            userBusiness.remove(id, true);
        }
    }

    @PutMapping(value = "{id}")
    public User update(@PathVariable String id, @RequestBody @Valid User user) throws VipException {
        if ( ! id.equals(user.getEmail())) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, id, "User email do not match!");
        } else {
            userBusiness.update(user);
            return userBusiness.get(id);
        }
    }

    @PostMapping
    public User create(@RequestBody @Valid SignUpForm form) throws VipException {
        if (form.user.getId() != null) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "id", "ID must be empty!");
        }
        User u = authenticationBusiness.signup(form.user, form.comment, true, false, form.user.getGroups());
        return userBusiness.get(u.getId()); // to have the full data
    }
}
