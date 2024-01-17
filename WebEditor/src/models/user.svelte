<script lang="ts" context="module">
	export class User {
		name: string;
		sub: string;
		email: string;
		auth_time: number;
		token: string | undefined;

		constructor(name: string, sub: string, email: string, auth_time: number, token: string | undefined) {
			this.name = name;
			this.sub = sub;
			this.email = email;
			this.auth_time = auth_time;
			this.token = token;
		}
	}
</script>
