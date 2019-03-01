package winnersonx;

import java.util.ArrayList;

class Commands {
    static final ArrayList<String> commandList=new ArrayList<>();
    static void setupCommands(){
        commandList.add("list");
        commandList.add("get");
        commandList.add("roll");
        commandList.add("vup");
        commandList.add("vout");
        commandList.add("play");
        commandList.add("last");
        commandList.add("next");
        commandList.add("stop");
        commandList.add("vol");
        commandList.add("sqrt");
    }
}
