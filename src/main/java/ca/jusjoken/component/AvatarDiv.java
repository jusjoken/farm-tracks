/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;

/**
 *
 * @author birch
 */
public class AvatarDiv extends Div{
    private Avatar avatar = new Avatar();

    public AvatarDiv(Avatar avatar) {
        this.avatar = avatar;
        add(avatar);
    }

    public Avatar getAvatar() {
        return avatar;
    }

    public void setAvatar(Avatar avatar) {
        this.avatar = avatar;
    }
    
    
}
