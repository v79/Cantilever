<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import { parseResString, CantileverProject, type ImgRes } from '$lib/models/project';
	import { stringify } from 'yaml';

	export const CLEAR_PROJECT = new CantileverProject(
		'',
		'',
		'dd/MM/yyyy',
		'dd/MM/yyyy HH:mm:ss',
		new Map<string, ImgRes>(),
		new Map<string, string>(),
		''
	);

	function createProjectStore() {
		const { subscribe, set, update } = writable<CantileverProject>(CLEAR_PROJECT);

		return {
			subscribe,
			set,
			update,
			clear: () => set(CLEAR_PROJECT)
		};
	}

	// This store manages the overall project
	export const project = createProjectStore();

	export async function fetchProject(
		token: string,
		projectName: string
	): Promise<CantileverProject | Error> {
		console.log('projectStore: Fetching project ' + projectName);
		try {
			const response = await fetch('https://api.cantilevers.org/project/load/' + projectName, {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				const data = await response.json();
				var tmpResolutions = Object.entries(data.data.imageResolutions); // Array[key, value]
				var imageRestMap: Map<string, ImgRes> = new Map<string, ImgRes>();
				for (const iR of tmpResolutions) {
					imageRestMap.set(iR[0], parseResString(iR[1] as string));
				}
				var tmpAttributes = Object.entries(data.data.attributes);
				var attributeMap: Map<string, string> = new Map<string, string>();
				for (const attr of tmpAttributes) {
					attributeMap.set(attr[0], attr[1] as string);
				}

				var tmpProject = new CantileverProject(
					data.data.projectName,
					data.data.author,
					data.data.dateFormat,
					data.data.dateTimeFormat,
					imageRestMap,
					attributeMap,
					data.data.domain
				);
				project.set(tmpProject);
				return data.data;
			} else {
				throw new Error('Failed to fetch project');
			}
		} catch (error) {
			console.log(error);
			return error as Error;
		}
	}

	// save changes to the project
	export async function saveProject(
		project: CantileverProject,
		token: string
	): Promise<CantileverProject | Error> {
		console.log('projectStore: Saving project');
		let yaml = stringify(project);
		try {
			const response = await fetch('https://api.cantilevers.org/project/', {
				method: 'PUT',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`,
					'Content-Type': 'application/yaml'
				},
				body: yaml,
				mode: 'cors'
			});
			if (response.ok) {
				const data = await response.json();
				return data.data;
			} else {
				throw new Error('Failed to save project');
			}
		} catch (error) {
			console.log(error);
			return error as Error;
		}
	}

	// yes, this is almost identical to saveProject, but it's a different endpoint
	export async function createProject(
		project: CantileverProject,
		token: string
	): Promise<CantileverProject | Error> {
		console.log('projectStore: Saving project');
		let yaml = stringify(project);
		try {
			const response = await fetch('https://api.cantilevers.org/project/new', {
				method: 'POST',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`,
					'Content-Type': 'application/yaml'
				},
				body: yaml,
				mode: 'cors'
			});
			if (response.status === 409) {
				throw new Error('Project already exists');
			} else if (response.ok) {
				const data = await response.json();
				return data.data;
			} else {
				throw new Error('Failed to save project');
			}
		} catch (error) {
			console.log('I caught an error and I am expecting it to be the 409 that the server sent');
			return error as Error;
		}
	}

	// get a list of projects
	export async function fetchProjectList(token: string): Promise<Map<string, string> | Error> {
		console.log('projectStore: Fetching project list');
		try {
			const response = await fetch('https://api.cantilevers.org/project/list', {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				const data = await response.json();
				let array = Object.entries(data.data);
				let projectList: Map<string, string> = new Map<string, string>();
				for (const p of array) {
					projectList.set(
						String((p[1] as { second: string }).second),
						String((p[1] as { first: string }).first)
					);
				}
				return projectList;
			} else {
				throw new Error('Failed to fetch project list');
			}
		} catch (error) {
			console.log(error);
			return error as Error;
		}
	}
</script>
