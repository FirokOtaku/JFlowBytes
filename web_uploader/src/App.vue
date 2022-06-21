<!--suppress HtmlUnknownTag, UnnecessaryContinueJS, UnnecessaryLabelOnBreakStatementJS -->
<style scoped>

.video-info-card {
	padding: 4px 12px;
}

.video-title {
	font-weight: bold;
	text-align: left;

	margin: 4px 2px 8px 2px;
}

.not-important-row {
	width: 100%;
	margin: 0;
	padding: 0;

	font-size: 85%;
	color: #c0bfbf;

	transition: color 0.3s;
}

.not-important-row:hover {
	color: #414141;
}

.red-dot {
	color: #d73b3d;
}

.upload-dialog-col-big {
	border: 1px solid #dddfe5;
	border-radius: 3px;
	padding: 12px;
}

.upload-dialog-col {
	text-align: left;
	min-height: 50px;
	display: flex;
	flex-direction: row;
	flex-wrap: nowrap;
	justify-content: space-between;
	align-items: stretch;
	align-content: stretch;
}

.upload-dialog-col-title {
	width: 70px;
	flex-grow: 0;
	flex-shrink: 0;
	margin-right: 10px;
	padding-top: 2px;
}

.mt15 {
	margin-top: 15px;
}
.mt10 {
	margin-top: 10px;
}

.upload-dialog-col-body {
	flex-grow: 1;
	flex-shrink: 0;

	display: block;
}

.el-col {
	margin-bottom: 16px;
}

.video-thumbnail-box {
	position: relative;
	height: 155px;
	display: flex;
	flex-direction: row;
	flex-wrap: nowrap;
	justify-content: center;
	align-items: center;
	align-content: center;
	overflow: hidden;
	cursor: pointer;
}

.video-thumbnail-image {
	width: 200px;
	height: 135px;
	border-radius: 6px;
}

.video-thumbnail-shadow {
	position: absolute;
	width: 200px;
	height: 135px;
	background-color: black;
	opacity: 0.35;
	border-radius: 6px;

	transition: opacity 0.3s;
}

.video-thumbnail-box:hover .video-thumbnail-shadow {
	opacity: 0.08;
}

.video-play-button {
	position: absolute;
	z-index: 1;
	font-size: 56px;
	color: #ffffff;
}
</style>

<template>
	<div style="width: 100%; ">
		<div style="display: flex; margin-bottom: 4px">
			<div style="flex-shrink: 0; flex-grow: 1; padding-top: 4px">视频列表</div>
			<div>
				<table style="width: 660px; flex-shrink: 0; flex-grow: 0">
					<tr>
						<td>
							<el-select v-model="filterTarget" clearable placeholder="储存方式">
								<el-option label="MinIO" value="minio"/>
								<el-option label="七牛云" value="qiniu"/>
								<el-option label="本地文件系统" value="filesystem"/>
							</el-select>
						</td>
						<td>
							<el-input v-model="filterFileName" clearable placeholder="文件名" type="text"/>
						</td>
						<td>
							<el-input v-model="filterBucketName" clearable placeholder="存储位置" type="text"/>
						</td>
						<td>
							<el-button style="background-color: #d3e6ee"
							           @click="clickRefreshRecords()">
								<el-icon :size="16"><refresh/></el-icon>
								<!-- todo 图标没法显示 -->
								查询
							</el-button>
						</td>
						<td>
							<el-badge :value="countErrorWorkflow ? countErrorWorkflow : countFinishedWorkflow ? countFinishedWorkflow : countActiveWorkflow ? countActiveWorkflow : countNotStartedWorkflow"
							          :max="99"
							          :hidden="!(countErrorWorkflow || countFinishedWorkflow || countActiveWorkflow || countNotStartedWorkflow)"
							          :type="countErrorWorkflow ? 'danger' : countFinishedWorkflow ? 'success' : countActiveWorkflow ? 'primary' : 'warning'">
								<el-button type="primary"
								           @click="isUploadModalOpen=true">
									<el-icon :size="16"><upload-filled/></el-icon>
									上传视频
								</el-button>
							</el-badge>
						</td>
					</tr>
				</table>
			</div>

		</div>

		<template v-if="pageRecord.records && pageRecord.records.length">
			<el-row :gutter="12" :v-loading="isLoadingRecordList">

				<el-col v-for="record in pageRecord.records" :key="record.id" :lg="6" :md="6" :sm="8"
				        :xl="4"
				        :xs="12">
					<el-card :body-style="{ padding: '0px'}"
					         class="video-info-card"
					         shadow="hover">
						<el-tooltip :content="record.fileName" placement="top">
							<div class="video-title">
								{{
									record.fileName && record.fileName.length > 15 ? record.fileName.substr(0, 15) + '...' : record.fileName
								}}
							</div>
						</el-tooltip>

						<div class="video-thumbnail-box"
						     @click="clickPreviewRecord(record)">
							<el-image :alt="record.fileName"
							          :src="record.urlThumbnail"
							          class="video-thumbnail-image"
							          fit="cover"
							>
								<template #error>
									<div style="text-align: center; padding-top: 16px; color: darkslategray">
										<div>
											<el-icon :size="64" color="black">
												<close/>
											</el-icon>
										</div>
										<div style="font-size: 40%">
											加载预览图失败
										</div>
									</div>
								</template>
								<template #placeholder>
									<div style="text-align: center; padding-top: 16px;">
										<div>
											<el-icon :size="64" color="black">
												<Picture/>
											</el-icon>
										</div>
										<div style="font-size: 40%">
											预览图加载中
										</div>
									</div>
								</template>
							</el-image>
							<div class="video-thumbnail-shadow">
							</div>
							<el-icon class="video-play-button"
							         :size="64" color="white">
								<video-play/>
							</el-icon>
						</div>

						<div>
							<div style="text-align: left">
								<table>
									<tr class="not-important-row">
										<td>
											位置
										</td>
										<td>
											<span v-if="record.target === 'minio'">MinIO</span>
											<span v-else-if="record.target === 'qiniu'">七牛云</span>
											<span v-else-if="record.target === 'filesystem'">本地文件存储</span>
											<span v-else>其它</span>
											/
											{{ record.bucketName }}
										</td>
									</tr>
									<tr class="not-important-row">
										<td>
											大小
										</td>
										<td>
											<fileSize :size="record.fileSize"></fileSize>
										</td>
									</tr>
									<tr class="not-important-row">
										<td>
											时间
										</td>
										<td>{{ record.uploadTimeStr }}</td>
									</tr>
								</table>
							</div>
							<div style="text-align: right; user-select: none">
								<el-button :underline="false" type="primary" link size="small"
								           @click="clickRenameRecord(record)">
									<el-icon :size="12"><edit/></el-icon>
									重命名
								</el-button>

								<el-popconfirm title="你确定要删除?"
								               confirm-button-text="删除"
								               confirm-button-type="danger"
								               cancel-button-text="取消"
								               cancel-button-type="default" @confirm="clickDeleteRecord(record)">
									<template #reference>
										<el-button type="danger" link size="small" style="margin-bottom: 2px">
											<el-icon :size="12"><delete-filled/></el-icon>
											删除
										</el-button>
									</template>
								</el-popconfirm>

							</div>

						</div>
					</el-card>
				</el-col>
			</el-row>
			<el-row :gutter="12">
				<el-col :span="24">
					<el-pagination :current-page="pageRecord.current"
					               style="justify-content: center"
					               @size-change="_handleSizeChange"
					               :page-sizes="[12, 24, 36, 48]"
					               layout="sizes, prev, pager, next, jumper, total"

					               :page-count="pageRecord.pages"
					               :page-size="pageRecord.size"
					               :total="pageRecord.total"
					               background
					               @current-change="clickChangeRecordPage($event)"
					/>
				</el-col>
			</el-row>
		</template>
		<template v-else>
			<el-empty :image-size="200" description="暂无记录"/>
		</template>

		<el-dialog :close-on-click-modal="false"
		           :model-value="isUploadModalOpen"
		           :show-close="false"
		           width="60%"
		           title="上传视频"
		           top="40px"
		           @close="closeUploadModal()">
			<div class="upload-dialog-col-big">
				<el-row class="mt10">
					<el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" >
						<div class="upload-dialog-col">
							<div class="upload-dialog-col-title">
								<span class="red-dot">*</span>
								储存方式:
							</div>
							<div class="upload-dialog-col-body">
								<el-select v-if="uploadTargetList.length"
								           v-model="uploadTarget"
								           :loading="isLoadingUploadTargetList"
								           default-first-option
								           placeholder="选择上传位置"
								           size="small"

								           style="width: 80%;" value-key="full">
									<el-option
										v-for="info in uploadTargetList"
										:key="info.full"
										:label="info.display"
										:value="info">
									</el-option>
								</el-select>
								<span v-else>
              无可用存储空间, 请先在配置文件中添加
            </span>
							</div>
						</div>
					</el-col>

					<el-col :lg="24" :md="24" :sm="12" :xl="12" :xs="12">
						<div class="upload-dialog-col">
							<div class="upload-dialog-col-title">
								<span class="red-dot">*</span>
								选择文件:
							</div>
							<div class="upload-dialog-col-body">
								<el-button size="small"
								           @click="$refs['filesToUpload'].click()"
								>
									<el-icon :size="12"><upload/></el-icon>
									选择文件
								</el-button>
								<div class="el-upload__tip">
									支持文件格式 <br>
									.mp4, .m4v, .flv, <br>
									.avi, .mpeg, .mpg
									<!--                  .mp4, .mpeg, .mov, .m4v, .mpg,<br>-->
									<!--                  .avi, .asx, .ogm, <br>-->
									<!--                  .wmv, .webm, .ogv <br>-->
								</div>

								<input ref="filesToUpload"
								       style="display: none"
								       type="file" multiple
								       accept="video/mp4, video/mpeg, video/mpeg4-generic, video/H263, video/H264, video/H265, video/avi, video/x-flv"
								       @change="uploadFileChanged($event)">

							</div>
						</div>
					</el-col>
				</el-row>
			</div>

			<div class="upload-dialog-col upload-dialog-col-big mt15">
				<el-table :border="true"
				          :data="listWorkflow"
				          :fit="true"
				          :row-key="(row) => row.iid"
				          :default-sort="{'statusWorkflow': 'ascending'}"
				          :stripe="true"
				          height="400"
				          style="width: 100%">
					<!--          <el-table-column label="id" prop="id"/>-->
					<el-table-column align="center" label="文件名" prop="uploadFileName" width="250"/>
					<el-table-column label="文件大小" align="center" width="120">
						<template #default="scope" align="center">
							<fileSize :size="scope.row.fileSize"/>
						</template>
					</el-table-column>
					<el-table-column label="上传目标" align="center" width="140">
						<template #default="scope">
							<uploadTarget :target="scope.row.uploadTargetName"/>
						</template>
					</el-table-column>
					<el-table-column label="桶" align="center"
					                 prop="uploadBucketName"
					                 width="160"/>
					<el-table-column sortable :sort-method="sortWorkflow"
					                 label="上传进度" prop="statusWorkflow"
					                 align="center" width="160">
						<template #default="scope">

							<el-tooltip class="item"
							            effect="dark"
							            placement="top">
								<template #content>
									<div v-if="scope.row.sizeLog > SIZE_QUERY_LOG" style="max-width: 400px;">
										<i>...</i>
									</div>
									<div v-for="log in scope.row.listLog">
										<i>{{ log.message }}</i>
									</div>
								</template>

								<div>
									<div>
										<el-progress v-if="scope.row.currentProgress > 0 && scope.row.currentProgress < 100"
										             :percentage="scope.row.currentOperationName === 'jfb:finished' ? 100 : scope.row.currentProgress"
										             :stroke-width="8"/>
									</div>
									<div style="font-size: 45%; text-align: left; padding-left: 8px">
                    <span v-if="scope.row.statusWorkflow === 'wait-notify'" style="color: darkorange">
                      等待开始
                    </span>
										<span v-else-if="scope.row.statusWorkflow === 'wait-upload'" style="color: deepskyblue">
                      上传中
                    </span>
										<span v-else-if="scope.row.statusWorkflow === 'wait-finish'" style="color: dodgerblue">
                      处理中
                    </span>
										<span v-else-if="scope.row.statusWorkflow === 'error' && scope.row.otherFatalError === '用户中断任务'" style="color: rebeccapurple">
                      停止中
                    </span>
										<span v-else-if="scope.row.statusWorkflow === 'error'" style="color: darkred">
                      错误
                    </span>
										<span v-else-if="scope.row.statusWorkflow === 'finish'" style="color: seagreen">
                      完成
                    </span>

										<span v-if="scope.row.statusWorkflow === 'wait-upload'" style="color: deepskyblue">
                      <fileSize :size="scope.row.uploadSpeed"/>/s
                    </span>
									</div>
								</div>
							</el-tooltip>
						</template>
					</el-table-column>
					<el-table-column align="center">
						<template #default="scope">
							<el-button v-if="scope.row.currentOperationProgressTotal === 100"
							           @click="stopWorkflow(scope.row.iid)"
							           size="small" :type="scope.row.currentOperationProgressTotal === 100 ? 'success' : 'warning'">
								完成
							</el-button>
							<el-button v-else
							           @click="stopWorkflow(scope.row.iid)"
							           size="small" type="warning">
								中止
							</el-button>
						</template>
					</el-table-column>

				</el-table>
			</div>

			<div slot="footer" class="dialog-footer">
				<el-row :gutter="0">
					<el-col :xs="24" :sm="24" :md="24" :lg="12" :xl="12">
						<div v-if="countActiveWorkflow || countNotStartedWorkflow"
						     style="color: #f62e2e; font-size: 45%; padding: 0; width: 310px">
							各任务完成前请勿刷新或关闭页面, 否则未完成任务将失效
						</div>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="12" :xl="12" style="text-align: right; margin-top: 4px">
						<el-button type="primary" size="small"
						           v-if="countNotStartedWorkflow"
						           :loading="isStartingWorkflows"
						           @click="clickStartWorkflows()">开始上传</el-button>
						<el-button type="success"  style="margin-left: 4px" size="small"
						           v-if="countFinishedWorkflow"
						           @click="clearFinishedWorkflow()">清空已完成</el-button>
						<el-tooltip class="item"  style="margin-left: 4px"
						            effect="dark" placement="top"
						            :disabled="countActiveWorkflow <= 0"
						            content="正在上传的任务仍会在后台继续进行">
							<el-button @click="closeUploadModal" size="small" type="primary">最小化</el-button>
						</el-tooltip>
					</el-col>
				</el-row>
			</div>

		</el-dialog>

		<el-dialog :close-on-click-modal="false"
		           :model-value="isPreviewModalOpen"
		           fullscreen
		           title="视频预览"
		           @close="_closePreviewModal()"
		           @opened="_openedPreviewModal()"
		>
			<!-- 你看我对 ElementUI 这个破全屏模态框的高度设定无能为力的样子 -->
			<!-- 那么, 答案只有一个了: -->
			<!-- 回应我吧, fixed 之力! -->
			<div ref="preview-player-parent"
			     style="position: fixed; top: 60px; left: 40px; width: calc(100% - 80px); height: calc(100% - 120px)"></div>
			<!-- that just works -->
		</el-dialog>

		<!-- 0.1.26 by Firok 你看像不像是上次我转移代码的时候忘了把这块加上 -->
		<upload-status-ball v-show="countFinishedWorkflow || countErrorWorkflow || countActiveWorkflow"
		                    :status="currentStatus"
		                    :color-bar="currentStatus === 'success' ? '#13ce66' : currentStatus === 'exception' ? '#ff4949' : '#20a0ff'"
		                    :value="currentProgress"
		                    :speed="sumUploadSpeed"/>

	</div>
</template>

<script>
import fileSize from "./components/fileSize";
import uploadTarget from "./components/uploadTarget";
import {JFBServer, slicesOf} from "./components/JFBServer";
import {debounce} from "lodash";
import UploadStatusBall from "./components/UploadStatusBall";
import {Refresh} from '@element-plus/icons-vue/dist/index';


/**
 * 替换某个数组内的所有内容
 * */
function replace (array, ...elements)
{
	if (array && array.length)
	{
		array.splice(0, array.length);
	}
	if (elements && elements.length)
	{
		array.push(...elements);
	}
}

export default {
	name: "VideoStorage",
	components: {
		fileSize,
		uploadTarget,
		UploadStatusBall,

		Refresh,
	},

	data ()
	{
		return {
			server: new JFBServer(),

			// 分页大小
			queryPageSize: 12,

			// 视频预览相关
			isPreviewModalOpen: false,
			previewPlayer: null,
			previewRecord: null,
			previewError: null,

			// 加载视频列表用的字段
			filterTarget: null,
			filterBucketName: '',
			filterFileName: '',
			isLoadingRecordList: false,
			pageRecord: [],

			// 前台刷新工作流线程
			threadRefreshWorkflow: null,
			isRefreshWorkflow: false,
			// 上传工作流列表
			listWorkflow: [],
			// 上传目标
			uploadTarget: {},
			uploadTargetDump: null,
			uploadFileName: '',
			isUploadModalOpen: false,
			isCreatingWorkflow: false,

			// 上传目标列表相关
			isLoadingUploadTargetList: false,
			uploadTargetList: [],

			idRecordToDelete: null,
			isDeletingRecord: false,

			// 开始工作流
			isStartingWorkflows: false,

			// 统计当前总进度用的
			sumUploadSpeed: 0,
			currentProgress: 100,
			currentStatus: 'success',
		};
	},
	computed: {
		SIZE_QUERY_LOG ()
		{
			return 10;
		},
		/**
		 * 经过排序的工作流列表
		 * */
		sortedListWorkflow()
		{
			const ORDER = ['wait-notify', 'error', 'finish', 'wait-upload', 'wait-finish'];
			return this.listWorkflow.sort((a, b) =>
			{
				if (a.statusWorkflow === b.statusWorkflow)
				{
					// id的比较没什么好说的 随意比较就行
					if(a.iid > b.iid) return 1;
					else if(a.iid < b.iid) return -1;
					else return 0;
				}
				else
				{
					const orderA = ORDER.indexOf(a.statusWorkflow);
					const orderB = ORDER.indexOf(b.statusWorkflow);
					return orderA - orderB;
				}
			});
		},
		/**
		 * 活跃中的任务数量
		 * */
		countActiveWorkflow()
		{
			return this.listWorkflow
				.filter(wf => wf.statusWorkflow === 'wait-upload' || wf.statusWorkflow === 'wait-upload' || wf.statusWorkflow === 'wait-finish')
				.length;
		},
		countFinishedWorkflow()
		{
			return this.listWorkflow
				.filter(wf => wf.statusWorkflow === 'finish')
				.length;
		},
		countErrorWorkflow()
		{
			return this.listWorkflow
				.filter(wf => wf.statusWorkflow === 'error')
				.length;
		},
		countNotStartedWorkflow()
		{
			return this.listWorkflow
				.filter(wf => wf.statusWorkflow === 'wait-notify')
				.length;
		},
	},
	methods: {
		sortWorkflow(a, b)
		{
			const ORDER = ['wait-notify', 'error', 'finish', 'wait-upload', 'wait-finish'];
			if (a.statusWorkflow === b.statusWorkflow)
			{
				// id的比较没什么好说的 随意比较就行
				if(a.iid > b.iid) return 1;
				else if(a.iid < b.iid) return -1;
				else return 0;
			}
			else
			{
				const orderA = ORDER.indexOf(a.statusWorkflow);
				const orderB = ORDER.indexOf(b.statusWorkflow);
				return orderA - orderB;
			}
		},
		clickDeleteRecord (record)
		{
			if(this.isDeletingRecord) return;
			this.server.deleteRecord(record.id)
				.then(res =>
				{
					this.$notify.success({
						title: '删除成功',
					})
				})
				.catch(err =>
				{
					this.$notify.error({
						title: '删除失败',
						message: '后台返回错误信息: ' + err.message,
					})
				})
				.finally(() =>
				{
					this.isDeletingRecord = false;
					this.clickRefreshRecords();
				});
		},
		clickRenameRecord (record)
		{
			this.$prompt('请输入新文件名', '重命名记录', {
				confirmButtonText: '重命名',
				cancelButtonText: '取消',
				inputValue: record.fileName,
				inputPattern: /.{0,255}/,
				inputErrorMessage: '文件名格式不正确'
			})
				.then(({ value }) => {
					this.server.renameRecord(record.id, value)
						.then(res =>
						{
							this.$notify.success({
								title: '重命名成功',
							})
						})
						.catch(err =>
						{
							this.$notify.error({
								title: '重命名失败',
								message: '后台返回错误信息: ' + err.message,
							})
						})
						.finally(() => {
							this.clickRefreshRecords();
						});
				})
				.catch(() =>
				{
				});
		},

		// 用户点击预览某个记录
		clickPreviewRecord (record)
		{
			this.previewRecord = record;
			this.isPreviewModalOpen = true;
		},
		// 关闭预览框
		_closePreviewModal ()
		{
			this.isPreviewModalOpen = false;
			this.previewRecord = null;
			this.previewPlayer.dispose();
			this.previewPlayer = null;
		},
		// 预览模态框打开完成 开始加载视频控件
		_openedPreviewModal ()
		{
			const record = this.previewRecord;
			const domParent = this.$refs['preview-player-parent'];
			domParent.innerHTML = `<video id="preview-player"
class="video-js vjs-default-skin vjs-big-play-centered vjs-fill"
controls responsive disablePictureInPicture="true"></video>`;
			this.server.urlFile({
				target: record.target,
				bucketName: record.bucketName,
				fileName: record.idTask + '.m3u8',
			})
				.then(url =>
				{
					this.previewPlayer = videojs('preview-player', {
						language: 'zh-CN',
						controls: true,
						autoplay: false,
						autoSetup: true,
						responsive: true,
						sources: [{src: url,}],
					});
				})
				.catch(err =>
				{
					console.log('record err', err);
				});

		},

		async clickRefreshRecords (pageIndex)
		{
			if (pageIndex == null)
				pageIndex = this.pageRecord ? this.pageRecord.current : 1;
			const {
				filterTarget,
				filterBucketName,
				filterFileName,
			} = this;
			if (this.isLoadingRecordList) return;
			this.isLoadingRecordList = true;
			try
			{
				let page = await this.server.listRecords({
					filterTarget,
					filterBucketName,
					filterFileName,
					pageIndex,
					pageSize: this.queryPageSize,
				});
				if (page.records != null && page.records.length === 0 && page.total > 0)
				{
					// 目前如果检测到当前页没有数据
					// 但是数据库里还是有数据的
					// 就直接跳到最后一页
					pageIndex = page.pages;
					page = await this.server.listRecords({
						filterTarget,
						filterBucketName,
						filterFileName,
						pageIndex,
						pageSize: this.queryPageSize,
					});
				}
				page.filterTarget = filterTarget;
				page.filterBucketName = filterBucketName;
				page.filterFileName = filterFileName;
				for (const record of page.records)
				{
					const {
						target,
						bucketName,
						fileName,
						idTask,
						uploadTime,
					} = record;
					const uploadDate = new Date(uploadTime);
					record.uploadTime = uploadDate;
					record.uploadTimeStr = uploadDate.toLocaleString();
					// fixme 这里可能会出现大量频繁请求 可能需要控制一下
					record.urlThumbnail = await this.server.urlFile({
						target, bucketName, fileName: idTask + '.jpg',
					});
				}
				this.pageRecord = page;
			}
			catch (err)
			{
				this.$notify.error({
					title: '获取上传记录失败',
					message: '后台返回错误信息: ' + err,
				});
			}

			this.isLoadingRecordList = false;
		},
		async clickChangeRecordPage ($event)
		{
			const {pageRecord} = this;
			this.filterFileName = pageRecord.filterFileName;
			this.filterBucketName = pageRecord.filterBucketName;
			this.filterTarget = pageRecord.filterTarget;
			await this.clickRefreshRecords($event);
		},

		uploadFileChanged ($event)
		{
			try
			{
				const ref = this.$refs['filesToUpload'];
				const files = ref.files != null ? ref.files : [];
				if(files.length === 0) return;
				let totalSize = 0;
				for(let file of files)
				{
					this.createWorkflow(file, this.uploadTarget);
					totalSize += file.size;
				}
				if(totalSize > 100 * 1024 * 1024)
				{
					this.$notify.warning({
						title: '提示',
						message: '您选择的文件较大, 上传可能需要较长时间. 上传完成之前请不要离开当前页面.',
					});
				}
			}
			catch (ignored) { }
			finally
			{
				// 避免重复选择相同文件却不能触发change的问题
				this.$refs['filesToUpload'].value = null;
			}
		},

		/**
		 * 创建工作流 目前只能创建上传工作流
		 * */
		async createWorkflow (fileToUpload, uploadTarget)
		{
			let {
				listWorkflow,
				isCreatingWorkflow,
			} = this;
			if (isCreatingWorkflow || fileToUpload == null || uploadTarget == null) return;
			this.isCreatingWorkflow = true;
			let uploadBucketName = uploadTarget.bucket;
			let uploadTargetName = uploadTarget.target;
			// 决定上传文件名
			let uploadFileName = fileToUpload.name;
			if (uploadFileName.length <= 0)
				uploadFileName = fileToUpload.name;
			// 根据文件大小决定切片大小
			const fileSize = fileToUpload.size;
			let sliceSize;
			if (fileSize <= 10000000) // 小于10MB
				sliceSize = fileSize;
			else // 大于10MB
				sliceSize = 10000000;

			// 处理切片数据
			let slices = []; // 切片列表
			let statuses = []; // 切片上传状态
			for (let slice of slicesOf(fileToUpload, sliceSize))
			{
				slices.push(slice);
				statuses.push(false);
			}

			// 准备前台工作流数据对象
			const workflow = {
				file: fileToUpload,
				fileSize,
				slices,
				statuses,
				uploadFileName,
				uploadBucketName,
				uploadTargetName,
				// 工作流状态
				// wait-notify 等待开始
				// wait-upload 上传分片中
				// wait-finish 等待后台处理
				// finish 工作流执行完成
				// error 工作流执行出现错误 或者用户停止相关工作流
				statusWorkflow: 'wait-notify',
				// 上传速度监控
				uploadSpeed: 0, // 单位 字节
				remainTime: 0, // 单位 秒

				// 错误信息相关
				listUploadError: [], // 错误信息列表 上传时的错误信息都收集在这里
				otherFatalError: null, // 其它错误信息 如果出现这个 工作流状态肯定为 error 一般来说是用户停止工作流会出现这个

				// 下面这些是后台的状态信息
				// 在 wait-finish
				currentOperationName: '',
				currentOperationProgressNow: 0,
				currentOperationProgressTotal: 0,
				currentProgress: 0,
				isError: false,
				listLog: [],
				listOperationName: [],
				sizeLog: 0,
				id: null,
				iid: new Date().getTime() + '-' + Math.random(),
			};
			// 先添加到工作流列表
			listWorkflow.push(workflow);
			this.isCreatingWorkflow = false;
		},

		async _startWorkflow(workflow)
		{
			const {
				slices,
				uploadTargetName,
				uploadBucketName,
				uploadFileName,
				fileSize,
			} = workflow;
			// 调用后台接口 在后台创建工作流
			let id = null;
			try
			{
				id = await this.server.createWorkflow({
					listOperation: [
						"jfb:upload",
						"jfb:file-merge",
						"jfb:ffmpeg-transcode-m3u8",
						`jfb:${uploadTargetName}-storage`,
						"jfb:record",
					],
					mapContextInitParam: {
						'name_bucket': uploadBucketName,
						'count_slice': slices.length,
						// 下面这仨是记录器用的参数
						'name_target': uploadTargetName,
						'file_name': uploadFileName,
						'file_size': fileSize,
					},
				});
				// 在后台创建工作流成功 更新工作流状态
				workflow.statusWorkflow = 'wait-upload';
				workflow.id = id;
			}
			catch (err)
			{
				// 创建工作流失败 记录错误信息 更新工作流状态
				workflow.otherFatalError = err;
				workflow.statusWorkflow = 'error';
				this.$notify.error({
					title: '创建工作流失败',
					message: '服务器返回错误信息: ' + err,
				});
				return; // 工作流创建出错就不用继续执行了
			}

			let lastCounterTime = 0;
			let lastCounterSize = 0;
			function counter(indexSlice, currentProgressSize)
			{
				const nowTime = new Date().getTime();
				if(lastCounterTime === 0) lastCounterTime = nowTime - 5000; // 大概就这样吧 懒得管了
				const intervalTime = Math.max(nowTime - lastCounterTime, 1);
				lastCounterTime = nowTime;

				let preSlicesTotalSize = 0; // 前面上传的分片 总容量
				for(let step = 0; step < indexSlice; step++)
				{
					preSlicesTotalSize += slices[step].size;
				}

				const totalProgressSize = preSlicesTotalSize + currentProgressSize;
				const intervalSize = Math.max(totalProgressSize - lastCounterSize, 0);
				lastCounterSize = totalProgressSize;

				// 计算上传速度和剩余时间
				const uploadSpeed = Math.ceil(intervalSize / (intervalTime / 1000));
				const remainTime = Math.ceil((fileSize - totalProgressSize) / uploadSpeed);
				workflow.uploadSpeed = uploadSpeed;
				workflow.remainTime = remainTime;

				// console.log(
				//   'intervalTime', intervalTime,
				//   'intervalSize', intervalSize,
				//   'speed', uploadSpeed,
				// )
			}

			// 开始上传
			// 这个地方在异步里做控制
			// 如果前台取消上传或者后台断了就停止上传
			FOR_SLICES: for (let step = 0; step < slices.length; step++)
			{
				if(workflow.statusWorkflow !== 'wait-upload')
				{
					return;
				}

				try // 上传切片
				{
					let slice = slices[step];

					let retStatus = await this.server.serviceUploadSlice({
						idWorkflow: id,
						indexSlice: step,
						slice,
					}, ($event) => {
						counter(step, $event.loaded);
					});

					let isSuccess = retStatus[step];
					workflow.statuses[step] = isSuccess
					if (isSuccess) // 切片上传成功
					{
						// 暂时不做处理
						continue;
					}
					else
					{
						// 调用接口成功 但是切片上传失败
						throw '上传出错';
					}
				}
				catch (err)
				{
					// 上传切片过程如果出现任何错误 就记录下相关错误信息
					workflow.listUploadError.push(err);
					// 调整step 失败重传
					step--;

					// 如果累计出现3次错误 停止工作流
					if (workflow.listUploadError.length >= 3)
					{
						this.$notify.error({
							title: '切片上传失败',
							message: '上传出错次数过多, 停止继续上传',
						});
						workflow.otherFatalError = '切片上传过程出现错误';
						workflow.statusWorkflow = 'error';
						break FOR_SLICES;
					}
				}

				if(workflow.statusWorkflow !== 'wait-upload')
				{
					return;
				}
			}

			// 上传完成 这个时候前台只需要轮询后台结果即可
			workflow.statusWorkflow = 'wait-finish';
			workflow.uploadSpeed = 0;
			workflow.remainTime = 0;
		},
		clickStartWorkflows()
		{
			this.isStartingWorkflows = true;
			for(let workflow of this.listWorkflow.filter(wf => wf.statusWorkflow === 'wait-notify'))
			{
				this._startWorkflow(workflow).finally(() => {});
			}
			this.isStartingWorkflows = false;
		},

		/**
		 * 根据后台传回的工作流数据列表
		 * 更新前台工作流数据信息
		 * */
		updateWorkflow (mapWorkflowStatus)
		{
			const {listWorkflow} = this;
			// 后台工作流id列表
			const listIdBack = Object.keys(mapWorkflowStatus);
			// 当前前台还存着的工作流id列表
			const listIdFront = listWorkflow.map(wf => wf.id).filter(id => id != null);
			// 比较两侧数据
			// 正常情况下肯定是后台id列表是前台列表的子集
			let shouldRefreshRecord = false; // 是否需要刷新上传记录列表
			for (let idBack of listIdBack)
			{
				// 如果前台工作流列表中有这个id
				if (listIdFront.includes(idBack))
				{
					// 找到前台工作流数据
					const wf = listWorkflow.find(wf => wf.id === idBack);
					// 拿到后台工作流数据
					const status = mapWorkflowStatus[idBack];

					// 更新工作流状态
					wf.currentOperationName = status.currentOperationName;
					wf.currentOperationProgressNow = status.currentOperationProgressNow;
					wf.currentOperationProgressTotal = status.currentOperationProgressTotal;
					wf.currentProgress = status.currentProgress;
					wf.isError = status.error;
					replace(wf.listLog, ...status.listLog);
					replace(wf.listOperationName, ...status.listOperationName);
					wf.sizeLog = status.sizeLog;

					if(wf.isError)
						wf.statusWorkflow = 'error';
					else if(wf.currentOperationName === 'jfb:finished' || wf.currentProgress >= 100)
					{
						wf.statusWorkflow = 'finish';
						wf.currentOperationProgressTotal = 100;
						shouldRefreshRecord = true; // 如果出现上传完成的任务就刷新上传记录列表
					}
				}
					// 前台不包含的话
					// 说明这个工作流已经在前台被删掉了
				// 这里需要调用后台接口 通知后台把这个也删掉
				else
				{
					// console.log('前台找不到此id, 开始删除', idBack);
					// this.server.deleteWorkflow(idBack).finally(() => {});
				}
			}
			// 更新完成
			if(shouldRefreshRecord) // 看看需不需要刷新上传记录列表
				this.clickRefreshRecords();
		},
		/**
		 * 停止并删除工作流
		 * */
		async stopWorkflow (iidWorkflow)
		{
			if (this.isRefreshWorkflow || iidWorkflow == null)
				return;

			const {listWorkflow} = this;
			// 根据内部id寻找工作流
			let workflow = null;
			let indexWorkflow = null;
			for(let step = 0; step < listWorkflow.length; step++)
			{
				let wf = listWorkflow[step];
				if(wf.iid === iidWorkflow)
				{
					workflow = wf;
					indexWorkflow = step;
					break;
				}
			}
			if(workflow == null) return;

			this.isRefreshWorkflow = true;
			workflow.otherFatalError = '用户中断任务';
			workflow.statusWorkflow = 'error';
			if(workflow.id == null) // 还没开始的任务
			{
				listWorkflow.splice(indexWorkflow, 1);
				this.isRefreshWorkflow = false;
				return;
			}
			try
			{
				await this.server.deleteWorkflow(workflow.id);
			}
			catch (ignored)
			{
				// this.$notify.error({
				//   title: '中断工作流失败',
				//   message: '服务器返回错误信息: ' + err,
				// });
			}
			finally
			{
				this.$notify.success({
					title: '成功',
				});
				listWorkflow.splice(indexWorkflow, 1);
				this.isRefreshWorkflow = false;
			}
		},
		async clearFinishedWorkflow()
		{
			const { listWorkflow, countFinishedWorkflow } = this;
			if(countFinishedWorkflow <= 0) return;
			const listIid = listWorkflow
				.filter(wf => wf.statusWorkflow === 'finish')
				.map(wf => wf.iid);
			for(let iid of listIid)
			{
				await this.stopWorkflow(iid);
			}
		},

		/**
		 * 更新工作流列表
		 * */
		refreshWorkflowList ()
		{
			// 获取目前所有工作流列表
			// 正在获取工作流状态
			// 或者当前没有打开上传模态框的话就不更新状态
			const {listWorkflow, isRefreshWorkflow} = this;
			if (isRefreshWorkflow) return; // 不管上传任务列表是否打开了 只要当前有任务就一直更新状态
			this.isRefreshWorkflow = true;

			const listWorkflowId = listWorkflow
				.filter(wf => wf.id != null && (wf.statusWorkflow === 'wait-finish' || wf.statusWorkflow === 'wait-upload'))
				.map(wf => wf.id);
			if (listWorkflowId.length === 0)
			{
				this.isRefreshWorkflow = false;
				return;
			}

			// 根据工作流列表id获取相关工作流信息
			this.server.listCurrentWorkflows(this.SIZE_QUERY_LOG, ...listWorkflowId)
				.then(mapStatus =>
				{
					// 更新前台工作流数据
					this.updateWorkflow(mapStatus);
				})
				.catch(err =>
				{
					this.$notify.error({
						title: '更新工作流数据出错',
						message: '后台返回错误信息' + err,
					})
				})
				.finally(() =>
				{
					this.isRefreshWorkflow = false;
					// 统计上传速度
					this.sumUploadSpeed = this.listWorkflow
						.filter(wf => wf.statusWorkflow === 'wait-upload' && wf.uploadSpeed > 0)
						.map(wf => wf.uploadSpeed)
						.reduce((a, b) => a + b, 0);
					// 统计上传进度
					let listWorkflow = this.listWorkflow.filter(wf => wf.statusWorkflow === 'wait-upload' || wf.statusWorkflow === 'wait-finish');
					let progressTotal = listWorkflow.length ? Math.ceil(
						listWorkflow
							.map(wf => wf.currentProgress)
							.reduce((a, b) => a + b, 0) / listWorkflow.length
					) : 100;
					if(progressTotal < 0) progressTotal = 0;
					else if(progressTotal > 100) progressTotal = 100;
					this.currentProgress = progressTotal;
					// console.log('当前进度', progressTotal);
					// 统计工作流状态
					if(this.countErrorWorkflow)
						this.currentStatus = 'exception';
					else if(this.countFinishedWorkflow)
						this.currentStatus = 'success';
					else
						this.currentStatus = undefined;
				});
		},

		/**
		 * 关闭上传模态框
		 * 这个时候更新一下当前的上传记录列表
		 * */
		closeUploadModal ()
		{
			this.isUploadModalOpen = false;
			this.clickRefreshRecords().finally(() =>
			{
			});
		},

		/**
		 * 分页控件改变页大小
		 * */
		_handleSizeChange(value)
		{
			this.queryPageSize = value;
			this.clickRefreshRecords().finally(()=>{});
		},


		/**
		 * 更新上传存储空间记录信息
		 * */
		refreshUploadTargetList ()
		{
			const {isLoadingUploadTargetList, uploadTargetList} = this;
			if (isLoadingUploadTargetList) return;
			this.isLoadingUploadTargetList = true;
			this.server.listUploadTarget()
				.then(map =>
				{
					let list = [];
					for (let target of Object.keys(map))
					{
						let buckets = map[target];
						for (let bucket of buckets)
						{
							list.push({
								target,
								bucket,
								full: target + ' - ' + bucket,
								display: (target === 'minio' ? 'MinIO' :
									target === 'qiniu' ? '七牛云' :
										target === 'filesystem' ? '本地文件存储' :
											target) + ' - ' + bucket,
							});
						}
					}

					list.sort((t1, t2) =>
					{
						if (t1.target > t2.target) return 1;
						else if (t1.target < t2.target) return -1;
						else if (t1.bucket > t1.bucket) return 1;
						else if (t1.bucket < t1.bucket) return -1;
						else return 0;
					});

					replace(uploadTargetList, ...list);
				})
				.catch(err =>
				{
					this.$notify.error({
						title: '加载存储空间记录信息出错',
						message: '后台返回错误信息: ' + err,
					})
				})
				.finally(() =>
				{
					this.isLoadingUploadTargetList = false;
					this.uploadTarget = this.uploadTargetList.length ? this.uploadTargetList[0] : null;
				});
		},

		routerLeave()
		{
			if(this.listWorkflow.length)
			{
				const answer =
					window.confirm('当前工作流列表不为空, 刷新或离开当前页面都会导致工作流失败, 是否继续?');
				if (!answer)
					return false
			}
			return true;
		},
		pageLeave($event)
		{
			if(!this.routerLeave())
				$event.returnValue = '当前工作流列表不为空, 刷新或离开当前页面都会导致工作流失败, 是否继续?';
		},
	},

	watch:
		{
			filterTarget() { this.debouncedWatchFilter(); },
			filterFileName() { this.debouncedWatchFilter(); },
			filterBucketName() { this.debouncedWatchFilter(); },
		},
	beforeRouteLeave ()
	{
		return this.routerLeave();
	},
	beforeRouteUpdate ()
	{
		return this.routerLeave();
	},

	created()
	{
		this.debouncedWatchFilter = debounce(() => {
			this.clickRefreshRecords();
		}, 1500);
		window.addEventListener('beforeunload', this.pageLeave);
	},
	destroyed()
	{
		window.removeEventListener('beforeunload', this.pageLeave);
	},
	mounted ()
	{
		this.clickRefreshRecords();
		this.refreshUploadTargetList();

		// 开始定时获取后台工作流状态
		this.threadRefreshWorkflow = setInterval(() =>
		{
			this.refreshWorkflowList();
		}, 1500);
	},
	beforeUnmount()
	{
		this.debouncedWatchFilter.cancel();
	},
	unmounted ()
	{
		// 停止线程
		clearInterval(this.threadRefreshWorkflow);
	}
}
</script>
