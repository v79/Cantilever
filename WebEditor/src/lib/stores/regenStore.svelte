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
				'Content-Length': '0',
				'cantilever-project-domain': projectDomain
			},
			body: JSON.stringify({}),
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
				'Content-Length': '0',
				'cantilever-project-domain': projectDomain
			},
			body: JSON.stringify({}),
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
				'Content-Length': '0',
				'cantilever-project-domain': projectDomain
			},
			body: JSON.stringify({}),
			mode: 'cors'
		});
		const data = await response.text();
		return data;
	}

	// clear specified cache
	export async function clearCache(
		cache: string,
		token: string,
		projectDomain: string
	): Promise<string> {
		console.log('regenStore: clearCache for ' + cache);
		const response = await fetch('https://api.cantilevers.org/generate/' + cache, {
			method: 'DELETE',
			headers: {
				Accept: 'text/plain',
				Authorization: 'Bearer ' + token,
				'Content-Type': 'application/json',
				'cantilever-project-domain': projectDomain
			},
			mode: 'cors'
		});
		const data = await response.text();
		return data;
	}
</script>
