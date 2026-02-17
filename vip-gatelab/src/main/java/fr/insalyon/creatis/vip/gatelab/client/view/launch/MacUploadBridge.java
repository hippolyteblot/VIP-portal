package fr.insalyon.creatis.vip.gatelab.client.view.launch;
import fr.insalyon.creatis.vip.core.client.view.Console;

public final class MacUploadBridge {


    private MacUploadBridge() {}

    public static void notifyUploadComplete(String inputList) {

        GateLabLaunchTab tab = GateLabLaunchTab.findActive();

        if (tab == null) {
            Console.log("No active GateLab found.");
            return;
        }

        tab.uploadMacComplete(inputList);
    }

}
