package me.marcdoesntexist.galaxymines.gui.actions;

@FunctionalInterface
public interface MenuAction {
    void execute(MenuActionContext context);
}
