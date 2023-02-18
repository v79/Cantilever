<script lang="ts">

    export let modalId: string;
	export let modalSize: string = 'xl';
</script>

<div
	class="modal fade fixed top-0 left-0  hidden h-full w-full overflow-y-auto overflow-x-hidden outline-none"
	id={modalId}
	tabindex="-1"
	aria-labelledby="exampleModalLabel"
	aria-hidden="true">
	<div
		class="modal-dialog modal-dialog-scrollable modal-{modalSize} pointer-events-none relative w-auto">
		<div
			class="modal-content pointer-events-auto relative flex w-full flex-col rounded-md border-none bg-white bg-clip-padding text-current shadow-lg outline-none">
			<div
				class="modal-header flex flex-shrink-0 items-center justify-between rounded-t-md border-b border-gray-200 p-4">
				<slot name="title" />
				<button
					type="button"
					class="btn-close box-content h-4 w-4 rounded-none border-none p-1 text-black opacity-50 hover:text-black hover:no-underline hover:opacity-75 focus:opacity-100 focus:shadow-none focus:outline-none"
					data-bs-dismiss="modal"
					aria-label="Close" />
			</div>
			<div class="preview modal-body relative space-y-2 p-4 text-sm">
				<slot name="body" />
			</div>
			<div
				class="modal-footer flex flex-shrink-0 flex-wrap items-center justify-end space-x-2 rounded-b-md border-t border-gray-200 p-4">
				<slot name="buttons">
					<!-- Default close button, can be overridden by slot -->
					<button
						type="button"
						class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
						data-bs-dismiss="modal">Close</button>
				</slot>
			</div>
		</div>
	</div>
</div>
