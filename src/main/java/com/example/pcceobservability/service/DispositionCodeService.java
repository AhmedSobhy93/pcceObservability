package com.example.pcceobservability.service;

import com.example.pcceobservability.model.DispositionCode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DispositionCodeService {

    private static final List<DispositionCode> CODES = List.of(
            new DispositionCode(1, "Abandoned In Network", "abandoned", true),
            new DispositionCode(2, "Abandoned In Local Queue", "abandoned", true),
            new DispositionCode(3, "Abandoned Ring", "abandoned", true),
            new DispositionCode(4, "Abandoned Delay", "abandoned", true),
            new DispositionCode(5, "Abandoned Interflow", "abandoned", true),
            new DispositionCode(6, "Abandoned Agent Terminal", "abandoned", true),
            new DispositionCode(7, "Short", "short_abandoned", true),
            new DispositionCode(10, "Disconnect/Drop No Answer", "disconnect_drop", true),
            new DispositionCode(13, "Handled Primary Route", "handled", false),
            new DispositionCode(14, "Handled Other", "handled", false),
            new DispositionCode(15, "Redirected/Rejected", "redirected", false),
            new DispositionCode(19, "Ring No Answer", "rona", true),
            new DispositionCode(26, "U-Abort", "abnormal_end", true),
            new DispositionCode(27, "Failed Software", "failure", true),
            new DispositionCode(28, "Blind Transfer", "transfer", false),
            new DispositionCode(29, "Announced Transfer", "transfer", false),
            new DispositionCode(30, "Conferenced", "conference", false),
            new DispositionCode(35, "Task Abandoned in Router", "task_abandoned", true),
            new DispositionCode(36, "Task Abandoned Before Offered", "task_abandoned", true),
            new DispositionCode(37, "Task Abandoned While Offered", "task_abandoned", true),
            new DispositionCode(39, "Cannot Obtain Task ID", "task_failure", true),
            new DispositionCode(40, "Agent Logged Out During Task", "task_abandoned", true),
            new DispositionCode(41, "Maximum Task Lifetime Exceeded", "task_timeout", true),
            new DispositionCode(42, "Application Path Went Down", "task_failure", true),
            new DispositionCode(52, "Called Party Disconnected", "disconnect", true),
            new DispositionCode(53, "Partial Call", "interim", false)
    );

    public List<DispositionCode> codes() {
        return CODES;
    }
}
