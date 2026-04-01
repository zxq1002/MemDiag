package com.memdiag.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Web controller for MemDiag UI pages.
 */
@Controller
public class WebController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "仪表盘 - MemDiag");
        model.addAttribute("activePage", "dashboard");
        return "index";
    }

    @GetMapping("/connections")
    public String connections(Model model) {
        model.addAttribute("pageTitle", "连接管理 - MemDiag");
        model.addAttribute("activePage", "connections");
        return "connections";
    }

    @GetMapping("/analysis/heap/{id}")
    public String heapAnalysis(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "堆内存分析 - MemDiag");
        model.addAttribute("activePage", "heap");
        model.addAttribute("connectionId", id);
        return "analysis/heap";
    }

    @GetMapping("/analysis/threads/{id}")
    public String threadAnalysis(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "线程分析 - MemDiag");
        model.addAttribute("activePage", "threads");
        model.addAttribute("connectionId", id);
        return "analysis/threads";
    }

    @GetMapping("/analysis/diagnose/{id}")
    public String diagnose(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "诊断报告 - MemDiag");
        model.addAttribute("activePage", "diagnose");
        model.addAttribute("connectionId", id);
        return "analysis/diagnose";
    }

    @GetMapping("/analysis/native/{id}")
    public String nativeAnalysis(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "堆外内存 - MemDiag");
        model.addAttribute("activePage", "native");
        model.addAttribute("connectionId", id);
        return "analysis/native";
    }

    @GetMapping("/snapshots/{id}")
    public String snapshots(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "快照管理 - MemDiag");
        model.addAttribute("activePage", "snapshots");
        model.addAttribute("connectionId", id);
        return "snapshots";
    }

    @GetMapping("/diff/{id}")
    public String diff(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "对比分析 - MemDiag");
        model.addAttribute("activePage", "diff");
        model.addAttribute("connectionId", id);
        return "diff";
    }

    @GetMapping("/report/{id}")
    public String report(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "报告导出 - MemDiag");
        model.addAttribute("activePage", "report");
        model.addAttribute("connectionId", id);
        return "report";
    }

    @GetMapping("/allocations/{id}")
    public String allocations(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "分配追踪 - MemDiag");
        model.addAttribute("activePage", "allocations");
        model.addAttribute("connectionId", id);
        return "allocations";
    }

    @GetMapping("/methods/{id}")
    public String methods(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "方法监控 - MemDiag");
        model.addAttribute("activePage", "methods");
        model.addAttribute("connectionId", id);
        return "methods";
    }

    @GetMapping("/agent/{id}")
    public String agent(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "Agent 管理 - MemDiag");
        model.addAttribute("activePage", "agent");
        model.addAttribute("connectionId", id);
        return "agent";
    }

    @GetMapping("/gc-roots/{id}")
    public String gcRoots(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "GC Roots - MemDiag");
        model.addAttribute("activePage", "gc-roots");
        model.addAttribute("connectionId", id);
        return "gc-roots";
    }
}
