package org.liamjd.cantilever.services

import org.liamjd.cantilever.models.dynamodb.Project

/**
 * Interface for DynamoDB operations related to Cantilever projects
 */
interface DynamoDBService {
    
    /**
     * Get a project by its domain
     * @param domain The project domain
     * @return The project if found, null otherwise
     */
    suspend fun getProject(domain: String): Project?
    
    /**
     * Save a project to DynamoDB
     * @param project The project to save
     * @return The saved project
     */
    suspend fun saveProject(project: Project): Project
    
    /**
     * Delete a project from DynamoDB
     * @param domain The project domain
     * @param projectName The project name
     * @return true if the project was deleted, false otherwise
     */
    suspend fun deleteProject(domain: String, projectName: String): Boolean
    
    /**
     * List all projects for a domain
     * @param domain The project domain
     * @return A list of projects for the domain
     */
    suspend fun listProjects(domain: String): List<Project>
    
    /**
     * List all projects
     * @return A list of all projects
     */
    suspend fun listAllProjects(): List<Project>
}