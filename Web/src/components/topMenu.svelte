<script>
    import LoginButton from './loginButton.svelte';
    import {activeStore} from '../stores/appStatusStore.svelte';
</script>

<nav
	class=" navbar navbar-light navbar-expand-lg relative flex w-full flex-wrap items-center justify-between bg-slate-800 py-4 text-gray-500 shadow-lg hover:text-gray-700 focus:text-gray-700 ">
	<div class="container-fluid flex w-full flex-wrap items-center justify-between px-6">
		<button
			aria-controls="navbarSupportedContent"
			aria-expanded="false"
			aria-label="Toggle navigation"
			class=" navbar-toggler border-0 bg-transparent py-2 px-2.5 text-gray-500 hover:no-underline hover:shadow-none focus:no-underline focus:shadow-none focus:outline-none focus:ring-0 "
			data-bs-target="#navbarSupportedContent"
			data-bs-toggle="collapse"
			type="button">
			<svg
				aria-hidden="true"
				class="w-6"
				data-icon="bars"
				data-prefix="fas"
				focusable="false"
				role="img"
				viewBox="0 0 448 512"
				xmlns="http://www.w3.org/2000/svg">
				<path
					d="M16 132h416c8.837 0 16-7.163 16-16V76c0-8.837-7.163-16-16-16H16C7.163 60 0 67.163 0 76v40c0 8.837 7.163 16 16 16zm0 160h416c8.837 0 16-7.163 16-16v-40c0-8.837-7.163-16-16-16H16c-8.837 0-16 7.163-16 16v40c0 8.837 7.163 16 16 16zm0 160h416c8.837 0 16-7.163 16-16v-40c0-8.837-7.163-16-16-16H16c-8.837 0-16 7.163-16 16v40c0 8.837 7.163 16 16 16z"
					fill="currentColor" />
			</svg>
		</button>
		<div class="navbar-collapse collapse flex-grow items-center" id="navbarSupportedContent">
			<!-- Left links -->
			<ul class="list-style-none navbar-nav mr-auto flex flex-col pl-0">
				<li class="nav-item p-2">
					<a class="nav-link p-0 text-gray-500 hover:text-gray-700 focus:text-gray-700" href="#"
						>Dashboard</a>
				</li>
				<li class="nav-item p-2">
					<a class="nav-link p-0 text-gray-500 hover:text-gray-700 focus:text-gray-700" href="#"
						>Team</a>
				</li>
				<li class="nav-item p-2">
					<a class="nav-link p-0 text-gray-500 hover:text-gray-700 focus:text-gray-700" href="#"
						>Projects</a>
				</li>
			</ul>
			<!-- Left links -->
		</div>
		<!-- Collapsible wrapper -->

		<div class="navbar-collapse collapse flex-grow items-center">
			<p>
				<strong>Current file:</strong>
				{$activeStore.activeFile} <strong>Is new:</strong>
				{$activeStore.isNewFile} <strong>Has changed: </strong>{$activeStore.hasChanged}
				<strong>Is valid: </strong>{$activeStore.isValid}
			</p>
		</div>

		<!-- Right elements -->
		<div class="relative flex items-center">
			<div class="dropdown relative">
				<a
					aria-expanded="false"
					class=" hidden-arrow dropdown-toggle mr-4 flex items-center text-gray-500 hover:text-gray-700 focus:text-gray-700 "
					data-bs-toggle="dropdown"
					id="dropdownMenuButton1"
					role="button">
					<svg
						aria-hidden="true"
						class="w-4"
						data-icon="bell"
						data-prefix="fas"
						focusable="false"
						role="img"
						viewBox="0 0 448 512"
						xmlns="http://www.w3.org/2000/svg">
						<path
							d="M224 512c35.32 0 63.97-28.65 63.97-64H160.03c0 35.35 28.65 64 63.97 64zm215.39-149.71c-19.32-20.76-55.47-51.99-55.47-154.29 0-77.7-54.48-139.9-127.94-155.16V32c0-17.67-14.32-32-31.98-32s-31.98 14.33-31.98 32v20.84C118.56 68.1 64.08 130.3 64.08 208c0 102.3-36.15 133.53-55.47 154.29-6 6.45-8.66 14.16-8.61 21.71.11 16.4 12.98 32 32.1 32h383.8c19.12 0 32-15.6 32.1-32 .05-7.55-2.61-15.27-8.61-21.71z"
							fill="currentColor" />
					</svg>
					<!-- <span class="absolute -mt-2.5 ml-2 rounded-full bg-red-700 py-0 px-1.5 text-xs text-white"
						>1</span> -->
				</a>
				<ul
					aria-labelledby="dropdownMenuButton1"
					class=" dropdown-menu absolute left-auto right-0 z-50 float-left m-0 mt-1 hidden min-w-max list-none rounded-lg border-none bg-white bg-clip-padding py-2 text-left text-base shadow-lg">
					<li>
						<a
							class=" dropdown-item block w-full whitespace-nowrap bg-transparent py-2 px-4 text-sm font-normal text-gray-700 hover:bg-gray-100 "
							>Action</a>
					</li>
					<li>
						<a
							class=" dropdown-item block w-full whitespace-nowrap bg-transparent py-2 px-4 text-sm font-normal text-gray-700 hover:bg-gray-100 "
							>Another action</a>
					</li>
					<li>
						<a
							class=" dropdown-item block w-full whitespace-nowrap bg-transparent py-2 px-4 text-sm font-normal text-gray-700 hover:bg-gray-100 "
							>Something else here</a>
					</li>
				</ul>
			</div>
			<LoginButton />
		</div>
		<!-- Right elements -->
	</div>
</nav>
