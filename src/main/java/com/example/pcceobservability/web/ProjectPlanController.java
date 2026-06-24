package com.example.pcceobservability.web;

import com.example.pcceobservability.model.ProjectTaskDto;
import com.example.pcceobservability.model.ProjectTaskUpdateRequest;
import com.example.pcceobservability.service.ProjectPlanService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class ProjectPlanController {

    private final ProjectPlanService projectPlanService;

    public ProjectPlanController(ProjectPlanService projectPlanService) {
        this.projectPlanService = projectPlanService;
    }

    @GetMapping("/project")
    public String project(Model model) {
        populate(model);
        return "project";
    }

    @GetMapping("/api/v1/project/tasks")
    @ResponseBody
    public List<ProjectTaskDto> tasks() {
        return projectPlanService.tasks();
    }

    @PostMapping("/api/v1/project/tasks")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ProjectTaskDto addTask(@RequestBody ProjectTaskUpdateRequest request) {
        return projectPlanService.add(request);
    }

    @PutMapping("/api/v1/project/tasks/{index}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ProjectTaskDto updateTask(
            @PathVariable int index,
            @RequestBody ProjectTaskUpdateRequest request) {
        return projectPlanService.update(index, request);
    }

    @PutMapping("/api/v1/project/tasks/id/{id}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ProjectTaskDto updateTaskById(
            @PathVariable long id,
            @RequestBody ProjectTaskUpdateRequest request) {
        return projectPlanService.updateById(id, request);
    }

    @DeleteMapping("/api/v1/project/tasks/id/{id}")
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void deleteTaskById(@PathVariable long id) {
        projectPlanService.deleteById(id);
    }

    @GetMapping("/api/v1/project/tasks/export.csv")
    @ResponseBody
    public ResponseEntity<String> exportCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pcce-project-plan.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(projectPlanService.csv());
    }

    private void populate(Model model) {
        List<ProjectTaskDto> tasks = projectPlanService.tasks();
        model.addAttribute("tasks", tasks);
        model.addAttribute("topicStats", projectPlanService.topicStats());
        model.addAttribute("resourceStats", projectPlanService.resourceStats());
        model.addAttribute("totalTasks", projectPlanService.totalTasks());
        model.addAttribute("completedCount", projectPlanService.completedCount());
        model.addAttribute("inProgressCount", projectPlanService.inProgressCount());
        model.addAttribute("onHoldCount", projectPlanService.onHoldCount());
        model.addAttribute("criticalOpenCount", projectPlanService.criticalOpenCount());
        model.addAttribute("resources", projectPlanService.resourceStats().stream()
                .map(resource -> resource.name())
                .toList());
    }
}
