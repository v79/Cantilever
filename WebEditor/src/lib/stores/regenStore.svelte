<script lang="ts" context="module">
	// this isn't really a store, but it is where I can call content regeneration routes

	// rebuild complete project metadata
	export async function rebuildAllMetadata(token: string, projectDomain: string): Promise<string> {
		console.log('regenStore: rebuildAllMetadata');
		const response = await fetch('https://api.cantilevers.org/metadata/rebuild', {
			method: 'PUT',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token,
				'X-Content-Length': '0',
				'cantilever-project-domain': projectDomain
			},
			mode: 'cors'
		});
		const data = await response.text();
		return data;
	}

	// rebuild all the posts
	export async function rebuildAllPosts(token: string, projectDomain: string): Promise<string> {
		console.log('regenStore: rebuildAllPosts');
		const response = await fetch('https://api.cantilevers.org/generate/post/*', {
			method: 'PUT',
			headers: {
				Accept: 'text/plain',
				Authorization: 'Bearer ' + token,
				'Content-Type': 'application/json',
				'X-Content-Length': '0',
				'cantilever-project-domain': projectDomain
			},
			mode: 'cors'
		});
		const data = await response.text();
		return data;
	}

	// rebuild all the pages
	export async function rebuildAllPages(token: string, projectDomain: string): Promise<string> {
		console.log('regenStore: rebuildAllPages');
		const response = await fetch('https://api.cantilevers.org/generate/page/*', {
			method: 'PUT',
			headers: {
				Accept: 'text/plain',
				Authorization: 'Bearer ' + token,
				'Content-Type': 'application/json',
				'X-Content-Length': '0',
				'cantilever-project-domain': projectDomain
			},
			mode: 'cors'
		});
		const data = await response.text();
		return data;
	}
</script>
