package com.nttdata.knot.iacapi.Models;
import java.util.List;

import com.nttdata.knot.iacapi.Models.UserPackage.User;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MicrosoftTeams {
    private Boolean enabled;
    private String name;
    private String description;
    private List<User> usersList;

}
