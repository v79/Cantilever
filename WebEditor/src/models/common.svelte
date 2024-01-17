<script lang="ts" context="module">
	/**
	 * Root class for all content nodes.
	 */
	export abstract class ContentNode {
		srcKey: string;
		lastUpdated: Date;
		url: string | undefined;

		constructor(srcKey: string, lastUpdated: Date, url: string | undefined) {
			this.srcKey = srcKey;
			this.lastUpdated = lastUpdated;
			this.url = url;
		}
	}
</script>
