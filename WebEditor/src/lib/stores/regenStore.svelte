<script lang="ts" context="module">
	// this isn't really a store, but it is where I can call content regeneration routes

	// rebuild complete project metadata
	export async function rebuildAllMetadata(token: string): Promise<string> {
		console.log('regenStore: rebuildAllMetadata');
		const response = await fetch('https://api.cantilevers.org/metadata/rebuild', {
			method: 'PUT',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token,
				'X-Content-Length': '0'
			},
			mode: 'cors'
		});
		const data = await response.text();
		return data;
	}
</script>
