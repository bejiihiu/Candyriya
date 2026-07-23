package kz.bejiihiu.candyriya.common.mod.server.permission;

import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;

public class CandyriyaDummyPermissible extends PermissibleBase {

    public static final ServerOperator DUMMY_OPERATOR = new ServerOperator() {
        @Override
        public boolean isOp() {
            return false;
        }

        @Override
        public void setOp(boolean b) {}
    };
    public CandyriyaDummyPermissible() {
        super(DUMMY_OPERATOR);
    }
}
