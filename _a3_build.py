# -*- coding: utf-8 -*-
"""生成实验3 UML类图 docx（v1.1，对齐手画版 9 个类）。"""
import os
from docx import Document
from docx.shared import Pt, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

OUT = r'E:\Study_Room_Reservation\docs\reports\实验3-UML类图.docx'
IMG = r'E:\Study_Room_Reservation\docs\assets\实验3_总类图.jpg'

doc = Document()

sec = doc.sections[0]
sec.page_width = Cm(21)
sec.page_height = Cm(29.7)
for m in ('left_margin', 'right_margin', 'top_margin', 'bottom_margin'):
    setattr(sec, m, Cm(2.5))

normal = doc.styles['Normal']
normal.font.name = '宋体'
normal.font.size = Pt(12)
normal.element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')


def _run(p, text, font='宋体', size=12, bold=False):
    r = p.add_run(text)
    r.font.name = font
    r.font.size = Pt(size)
    r.font.bold = bold
    r._element.rPr.rFonts.set(qn('w:eastAsia'), font)
    return r


def para(text, font='宋体', size=12, bold=False, align=None, space_before=None, indent=None):
    p = doc.add_paragraph()
    if align is not None:
        p.alignment = align
    if space_before is not None:
        p.paragraph_format.space_before = Pt(space_before)
    if indent is not None:
        p.paragraph_format.first_line_indent = Pt(indent)
    _run(p, text, font, size, bold)
    return p


def h1(text):
    return para(text, '黑体', 14, True, space_before=12)


def h2(text):
    return para(text, '黑体', 12, True, space_before=6)


def h3(text):
    return para(text, '黑体', 12, True, space_before=4)


def body(text):
    return para(text, '宋体', 12, False, indent=24)


def bullet(text):
    return para(text, '宋体', 12)


def caption(text):
    return para(text, '宋体', 9, False, WD_ALIGN_PARAGRAPH.CENTER)


def note(text):
    return para(text, '宋体', 11, False)


def table(headers, rows, widths=None, font_size=10.5):
    if doc.paragraphs:
        doc.paragraphs[-1].paragraph_format.keep_with_next = True
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = 'Table Grid'
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    trPr = t.rows[0]._tr.get_or_add_trPr()
    tbl_hdr = OxmlElement('w:tblHeader')
    tbl_hdr.set(qn('w:val'), 'true')
    trPr.append(tbl_hdr)
    hdr = t.rows[0].cells
    for i, h in enumerate(headers):
        hdr[i].text = ''
        p = hdr[i].paragraphs[0]
        _run(p, h, '宋体', font_size, True)
    for row in rows:
        cells = t.add_row().cells
        for i, v in enumerate(row):
            cells[i].text = ''
            p = cells[i].paragraphs[0]
            _run(p, str(v), '宋体', font_size, False)
    if widths:
        for i, w in enumerate(widths):
            for r in t.rows:
                r.cells[i].width = Cm(w)
    doc.add_paragraph()
    return t


# ================= 封面 =================
para('汕头大学实验报告', '黑体', 18, True, WD_ALIGN_PARAGRAPH.CENTER)
para('学院：数学与计算机学院         系：计算机系         专业：计算机科学与技术         成绩：________',
     '宋体', 9, False, WD_ALIGN_PARAGRAPH.CENTER)
para('姓名：易雨杰(2024611209)、叶梅冰(2024611038)   实验时间：2026年10月28日   教师签字：____________',
     '宋体', 9, False, WD_ALIGN_PARAGRAPH.CENTER)
para('实验3 UML类图', '黑体', 14, True, WD_ALIGN_PARAGRAPH.CENTER)
para('（自习室预约管理子系统）', '黑体', 14, True, WD_ALIGN_PARAGRAPH.CENTER)

# ================= 一、实验目的 =================
h1('一、实验目的')
body('1.掌握从业务需求与系统功能中识别类与对象的方法，理解名词法提取候选类的完整过程。')
body('2.掌握类的属性、操作的定义方式，以及职责分配的基本原则。')
body('3.掌握类间关联、聚合、依赖关系的表达，能在StarUML中绘制完整类图并导出。')
body('4.形成与接口契约一致的系统静态结构，为后续状态图、时序图与编码提供依据。')

# ================= 二、实验要求 =================
h1('二、实验要求')
body('1.分析系统业务流程与系统功能，定义系统的初步静态结构，找出重要的类和对象。')
body('2.使用StarUML建模工具绘制类图，并描述和建立类间关系。')
body('3.为每个类补充分析文档（类字典），说明职责、属性、操作与关联。')
body('4.提交实验报告。')

# ================= 三、主要实验工具 =================
h1('三、主要实验工具')
for line in [
    '●StarUML5.0（with cm）：安装路径D:\\StarUML，用于本项目全部UML图的建模与导出，启动前需设置Windows XP(Service Pack 2)兼容模式。',
    '●Git/GitHub：版本控制与团队协作，主干main，成员A（易雨杰）使用dev-A，成员B（叶梅冰）使用dev-B-new。',
    '●IntelliJ IDEA + JDK 17 + Maven：后续编码阶段使用（Maven Wrapper已随仓库提供）。',
    '●接口契约v0.6（docs/contract/接口契约.md）：本实验的业务字段、枚举取值、关键方法签名以契约为准，类图不另造名词。',
]:
    bullet(line)

# ================= 四、实验内容 =================
h1('四、实验内容及实验数据记录')

h2('（一）名词法提取候选类')
body('按指导书的建模步骤，先从需求文本中提取名词，再逐层筛选。')

h3('1.第一步：从需求中找出名词（候选对象）')
body('自习室、座位、座位号、座位区域、座位类型、学生、学号、手机号、预约、预约记录、预约号、预约时间、签到截止时间、签到、签退、签到记录、实际开始时间、实际结束时间、信用分、罚分、违约、值班管理员、系统管理员、用户、账号、用户名、密码、角色。')

h3('2.第二步：合并含义相同的名词，排除范围以外的名词')
table(['处理方式', '名词', '说明'], [
    ['合并', '预约／预约记录／预约号', '统一为「预约」这一对象，预约编号是它的标识属性'],
    ['合并', '签到记录／签退记录', '统一为「签到签退记录」，同一次使用只产生一条'],
    ['合并', '座位区域／座位类型', '统一为「座位类型」，类型名称即分类名称'],
    ['合并', '违约／罚分／信用分扣减', '都是信用分机制的表现，归入「信用分」属性与罚分字段'],
    ['排除', '自习室', '是整个系统本身，不是系统内的对象'],
    ['排除', '值班管理员／系统管理员', '不作为独立类，由「用户」对象的角色属性区分'],
], widths=[2.2, 5.0, 8.8])

h3('3.第三步：去掉只能作为类属性的名词')
body('日期、座位号、学号、手机号、用户名、密码、信用分、罚分、签到截止时间——这些都依附于某个对象存在，作为属性保留，不单独成类。')

h3('4.第四步：剩下的名词就是分析类')
body('用户、学生、座位类型、座位、预约、签到签退记录、信用分变动流水，共7个业务类；角色、座位状态、预约状态、信用分变动原因作为4个枚举。')

h2('（二）类的属性')
body('属性来源分三类：业务直接给出的字段（如座位号、学号）、状态类属性（如座位状态、预约状态）、关联类属性（如预约中的用户与座位引用）。')
table(['类', '属性', '类型', '说明'], [
    ['User', 'userId', 'Long', '用户唯一编号'],
    ['User', 'username', 'String', '登录名'],
    ['User', 'password', 'String', '登录密码'],
    ['User', 'name', 'String', '用户姓名'],
    ['User', 'role', 'Role', '角色，取值为枚举'],
    ['Student', 'studentId', 'String', '学号，唯一标识'],
    ['Student', 'name', 'String', '学生姓名'],
    ['Student', 'phone', 'String', '联系电话'],
    ['Student', 'creditScore', 'Integer', '信用分，爽约扣5、超时扣1、低于80禁约'],
    ['SeatType', 'typeName', 'String', '区域名称，如「单人自习区」'],
    ['SeatType', 'seatCount', 'Integer（derived）', '该类型座位总数，由Seat按typeName聚合派生，不单独存储；M01通过增删Seat记录维护，不直接改该字段'],
    ['Seat', 'seatId', 'Long', '座位唯一编号'],
    ['Seat', 'roomName', 'String', '所在房间名称'],
    ['Seat', 'seatNo', 'String', '座位号，唯一标识'],
    ['Seat', 'status', 'SeatStatus', '座位状态，取值为枚举'],
    ['Reservation', 'reservationId', 'Long', '预约唯一编号'],
    ['Reservation', 'userId', 'Long', '预约人编号'],
    ['Reservation', 'seatId', 'Long', '预约座位编号'],
    ['Reservation', 'createTime', 'Date', '预约提交时间'],
    ['Reservation', 'startTime', 'Date', '预约开始时间'],
    ['Reservation', 'endTime', 'Date', '预约结束时间'],
    ['Reservation', 'signInDeadline', 'Date', '签到截止时间（预约开始+15分钟）'],
    ['Reservation', 'status', 'ReservationStatus', '预约状态，取值为枚举'],
    ['CheckinRecord', 'recordNo', 'String', '记录号，唯一标识'],
    ['CheckinRecord', 'actualStart', 'Date', '实际签到时间'],
    ['CheckinRecord', 'actualEnd', 'Date', '实际签退时间'],
    ['CheckinRecord', 'penalty', 'Integer', '本次扣减信用分，无扣分为0'],
    ['CreditScoreLog', 'logId', 'Long', '流水唯一编号'],
    ['CreditScoreLog', 'studentId', 'String', '变动所属学生'],
    ['CreditScoreLog', 'reservationId', 'String', '关联预约号，可空（管理员手工调整时为空），便于追溯'],
    ['CreditScoreLog', 'changeValue', 'Integer', '本次变动分值（扣分记负，加分记正）'],
    ['CreditScoreLog', 'reason', 'CreditChangeReason', '变动原因，取值为枚举'],
    ['CreditScoreLog', 'createTime', 'Date', '变动发生时间'],
], widths=[2.6, 3.4, 3.0, 6.4])

body('状态与枚举补充（对应指导书「补充该类动态属性，如状态」）：')
table(['枚举', '取值', '含义'], [
    ['Role', 'STUDENT/DUTY_ADMIN/SYS_ADMIN', '学生／值班管理员／系统管理员'],
    ['SeatStatus', 'AVAILABLE/RESERVED/OCCUPIED/CLOSED', '可用／已预约／使用中／维修关闭'],
    ['ReservationStatus', 'BOOKED/CHECKED_IN/CHECKED_OUT/CANCELLED/NO_SHOW', '已预约／已签到／已签退／已取消／爽约未签到'],
    ['CreditChangeReason', 'NO_SHOW/LATE_CHECKOUT/OTHER', '爽约扣5／签退超时扣1／管理员手工调整'],
], widths=[3.4, 7.0, 5.2])

h2('（三）类的操作与职责分配')
body('按指导书「从动词、功能或系统责任中寻找操作，再合并筛选、合理分配」，把动词归到职责最贴近的类上。本类图以业务实体为主，把与自身状态直接相关的方法放在实体类上；跨实体的业务编排（如列表查询、多对象事务）在后续Service层实现，不在本图展开。')
table(['类', '操作', '说明'], [
    ['User', 'login(): boolean', '校验用户名密码，返回是否登录成功'],
    ['User', 'modifyInfo(): void', '修改本人资料'],
    ['Seat', 'updateStatus(newStatus: SeatStatus): void', '更新座位状态；OCCUPIED不得直接改AVAILABLE，必须先签退'],
    ['Reservation', 'cancel(): boolean', '主动取消预约，释放座位'],
    ['Reservation', 'checkIn(): boolean', '值班管理员在前台执行签到登记，置为已签到（Study-F01操作者为值班管理员，不是学生自助）'],
    ['Reservation', 'checkOut(): boolean', '值班管理员在前台执行签退登记，释放座位，判定是否超时并扣分'],
    ['Reservation', 'handleNoShow(): void', '超时未签到由系统判定爽约，状态置NO_SHOW，释放座位、扣5分并写流水'],
], widths=[2.8, 6.0, 6.6])
note('说明：handleNoShow是接口契约v0.5起补充、v0.6与类图对齐的关键入口。如果只靠学生点取消或管理员点签退，学生既不签到也不取消时座位会被永久占用，因此必须有一个独立的爽约判定入口。')

h2('（四）类间关系')
body('本图共6条业务关系 + 4条枚举依赖，如下：')
table(['关系', '两端', '多重性', '说明'], [
    ['聚合（空心菱形）', 'SeatType→Seat', '1:0..*', '一种座位类型包含0或多个座位；座位删除不影响类型'],
    ['关联', 'User→Reservation', '1:0..*', '一个用户可以有0或多条预约；一条预约只属于一个用户'],
    ['关联', 'Seat→Reservation', '1:0..*', '一个座位在不同时段可被多次预约；一条预约对应一个座位'],
    ['关联', 'User→Student', '1:0..1', '学生角色的账户对应一份学生档案；管理员账户无学生档案'],
    ['关联', 'Reservation→CheckinRecord', '1:0..1', '只有完成签到的预约才有签到签退记录'],
    ['关联', 'Student→CreditScoreLog', '1:0..*', '一个学生有多条信用分变动流水；每条流水只属于一个学生'],
    ['依赖（虚线）', 'User→Role', '—', 'User的role属性引用Role枚举'],
    ['依赖（虚线）', 'Seat→SeatStatus', '—', 'Seat的status属性引用SeatStatus枚举'],
    ['依赖（虚线）', 'Reservation→ReservationStatus', '—', 'Reservation的status属性引用ReservationStatus枚举'],
    ['依赖（虚线）', 'CreditScoreLog→CreditChangeReason', '—', 'CreditScoreLog的reason属性引用CreditChangeReason枚举'],
], widths=[2.6, 5.2, 2.0, 5.6])
body('关于泛化：用例图中「学生／值班管理员／系统管理员」泛化到抽象参与者「用户」，这是参与者层次；在类图中三者合并为User类，用Role枚举区分权限，因此类图不出现类之间的泛化箭头。这是本次建模相对用例图的一个有意简化。')

h2('（五）类图')
body('本图在StarUML 5.0中按上述7个业务类、4个枚举、6条关联（含1条聚合）、4条依赖建模，工程文件为docs/uml/实验3_总类图.uml，导出图片为docs/assets/实验3_总类图.jpg。')
if os.path.exists(IMG):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(IMG, width=Cm(15.5))
    caption('图3-1 自习室预约管理子系统总类图')
else:
    body('类图见图3-1（StarUML导出图，导出后插入本节）。')
    caption('图3-1 自习室预约管理子系统总类图')
body('图例说明：黄色矩形为类，顶部带<<enumeration>>标签的为枚举；实线为关联，空心菱形为聚合（贴在整体端），虚线箭头为依赖；线上数字为多重性。')

h2('（六）类字典')
body('按指导书「补充每个类的分析文档」的要求，逐类说明。')

classes = [
    ('1.User（用户账户）', [
        '职责：保存系统登录账户，区分三种角色，承载登录与资料修改。',
        '属性：userId、username、password、name、role（不存信用分，信用分在Student.creditScore）。',
        '操作：login()校验用户名密码；modifyInfo()修改本人资料。',
        '关联：与Reservation为1:0..*；与Student为1:0..1；依赖Role枚举。',
        '支撑用例：Study-U01登录、Study-C02在线预约、Study-C03取消预约。',
    ]),
    ('2.Student（学生档案）', [
        '职责：保存学生基本信息与信用分，是信用分扣分与禁约判定的落点。',
        '属性：studentId、name、phone、creditScore。',
        '操作：本类不绑方法，信用分校验在预约流程中由系统读取creditScore完成。',
        '关联：与User为0..1:1；与CreditScoreLog为1:0..*（一个学生有多条信用分变动流水）。',
        '支撑用例：Study-C02在线预约（信用分校验）、Study-M03预约记录与信用分管理。',
    ]),
    ('3.SeatType（座位类型）', [
        '职责：描述一类座位及其规模，是M01维护规则时的操作对象。',
        '属性：typeName、seatCount（派生属性，由Seat按typeName聚合统计，不单独持久化；M01维护座位数量是通过增删Seat记录实现的，不是直接改seatCount）。',
        '操作：无业务操作。',
        '关联：与Seat为聚合1:0..*，空心菱形在SeatType端。',
        '支撑用例：Study-M01座位区域与规则维护、Study-C01查询座位（按类型筛选）。',
    ]),
    ('4.Seat（座位）', [
        '职责：保存单个座位的编号、所在房间、类型与状态。',
        '属性：seatId、roomName、seatNo、status。',
        '操作：updateStatus(newStatus: SeatStatus)更新座位状态，传入目标状态。',
        '关联：与SeatType为0..*:1聚合；与Reservation为1:0..*；依赖SeatStatus枚举。',
        '状态流转：AVAILABLE→RESERVED（预约成功）→OCCUPIED（签到）→AVAILABLE（签退释放）；任意状态→CLOSED（转维修）；OCCUPIED不得直接改为AVAILABLE，必须先签退（契约v0.6约束）。',
        '支撑用例：Study-C01、Study-C02、Study-F01、Study-F02、Study-M02。',
    ]),
    ('5.Reservation（预约记录）', [
        '职责：保存一次预约的用户、座位、时段、签到截止时间与状态。',
        '属性：reservationId、userId、seatId、createTime（提交时间）、startTime（预约开始）、endTime（预约结束）、signInDeadline、status。',
        '操作：cancel()主动取消；checkIn()值班管理员在前台执行签到登记；checkOut()值班管理员执行签退并判定超时；handleNoShow()超时未签到由后台系统自动置NO_SHOW、释放座位、扣5分并写流水。',
        '关联：与User为0..*:1；与Seat为0..*:1；与CheckinRecord为1:0..1；依赖ReservationStatus枚举。',
        '状态流转：BOOKED→CHECKED_IN（签到）→CHECKED_OUT（签退）；BOOKED→CANCELLED（学生主动取消）；BOOKED→NO_SHOW（超时未签到，系统自动爽约）。',
        '支撑用例：Study-C02、Study-C03、Study-F01、Study-F02、Study-M03。',
    ]),
    ('6.CheckinRecord（签到签退记录）', [
        '职责：保存一次实际使用的起止时间与本次扣分，作为信用分变动的凭据。',
        '属性：recordNo、actualStart、actualEnd、penalty。',
        '操作：无业务操作。',
        '关联：与Reservation为0..1:1。',
        '支撑用例：Study-F01、Study-F02、Study-M03。',
    ]),
    ('7.CreditScoreLog（信用分变动流水）', [
        '职责：记录学生信用分的每一次增减，作为M03信用分流水页的展示数据；不直接改Student.creditScore，由业务流程在改分数的同时写一条流水。',
        '属性：logId、studentId、reservationId（可空，管理员手工调整时为空，便于追溯到具体预约）、changeValue（扣分记负、加分记正）、reason、createTime。',
        '操作：无业务操作，只作为流水记录。',
        '关联：与Student为0..*:1；依赖CreditChangeReason枚举。',
        '支撑用例：Study-M03预约记录与信用分管理、Study-I03超时扣分写流水。',
    ]),
    ('8.Role／SeatStatus／ReservationStatus／CreditChangeReason（枚举）', [
        '职责：替代大量布尔属性，把状态取值固定下来，防止出现非法状态字符串。',
        '取值：见（二）状态与枚举补充。',
        '说明：持久化时按英文常量名写入CSV，与契约第八节一致；ReservationStatus新增NO_SHOW与主动取消CANCELLED区分开，便于统计爽约率。',
    ]),
]
for title, lines in classes:
    h3(title)
    for line in lines:
        body(line)

h2('（七）实验2十二个用例到类的映射')
table(['用例编号', '用例名称', '主要承载类'], [
    ['Study-U01', '登录', 'User、Role'],
    ['Study-C01', '查询座位', 'Seat、SeatType、SeatStatus'],
    ['Study-C02', '在线预约', 'User、Student、Reservation、Seat'],
    ['Study-C03', '取消预约', 'Reservation.cancel、Seat.status'],
    ['Study-F01', '签到登记', 'Reservation.checkIn、Seat.status、CheckinRecord'],
    ['Study-F02', '签退释放', 'Reservation、Seat.status、CheckinRecord'],
    ['Study-M01', '座位区域与规则维护', 'SeatType、Seat'],
    ['Study-M02', '座位状态维护', 'Seat.updateStatus、SeatStatus'],
    ['Study-M03', '预约记录与信用分管理', 'Reservation、Student.creditScore、CreditScoreLog、CheckinRecord.penalty'],
    ['Study-I01', '校验座位与时段可用性', 'Seat.status、Reservation'],
    ['Study-I02', '核验预约记录', 'Reservation.status、ReservationId'],
    ['Study-I03', '判定超时并记录信用分扣减', 'Reservation.checkOut()超时判定 + CreditScoreLog(LATE_CHECKOUT, -1)'],
], widths=[2.6, 5.2, 8.0])
note('说明：用例中的跨对象业务编排（如「列出某时段全部可用座位」「扫描全部超时未签到预约」）在本类图中体现为相关实体的协作；具体服务类（ReservationService／SeatService／AuthService）与DAO层将在A-5、A-6编码阶段补出，不在本业务类图展开，避免分析阶段过早引入技术分层。')
note('爽约扣5分与上表I03（签退超时扣1分）是两条独立链路：爽约扣5分由后台服务定时调用Reservation.handleNoShow()处理，把预约状态从BOOKED置为NO_SHOW、释放座位，并写一条CreditScoreLog(NO_SHOW, -5)流水；签退超时扣1分由Reservation.checkOut()在F02流程内判定，写一条CreditScoreLog(LATE_CHECKOUT, -1)流水。')

# ================= 五、讨论 =================
h1('五、实验数据分析与讨论')

h2('（一）从用例图到类图的推导')
body('用例图回答「谁做什么」，类图回答「用什么做」。本实验的推导路径有三条：')
body('1.参与者收敛为类：用例图里的四个参与者（学生、值班管理员、系统管理员、抽象用户）在类图中收敛为User一个类加Role枚举。三个角色的差别只是权限，用枚举表达比建三个类更省，也避免出现只有role字段不同的重复类。')
body('2.用例动词分配到类：把「取消」「签到」「爽约判定」这些与预约自身状态强相关的动词放在Reservation上；把「更新状态」放在Seat上；把「登录」「改资料」放在User上。跨对象编排（如扫描全部超时预约）以方法形式handleNoShow挂在预约类上，后续再抽服务类。')
body('3.被包含用例落到方法内部：用例图中的三个被包含用例（校验座位与时段可用性、核验预约记录、判定超时并扣分）在类图中不是独立的类，而是预约/座位相关方法内部的一段校验逻辑。这与include关系的语义一致——被包含用例是被调用方，在代码里就是一段被复用的方法体。')

h2('（二）信用分机制的类图落点')
body('契约v0.4引入信用分、v0.6补流水表后，类图上有三处必须同时对齐：')
body('1.分存在哪里：Student.creditScore是当前权威分值；每一次增减都同步写一条CreditScoreLog流水（记录studentId、changeValue、reason、createTime），M03管理界面展示的就是这张流水表。User类不存信用分，避免账户和学生档案两边数据不一致。')
body('2.扣多少由谁决定：爽约扣5、超时扣1、低于80禁约、签到宽限15分钟——这些参数在M01维护用例中集中配置，类图上不单独建配置类，避免分析阶段过度设计；编码阶段再抽到常量类。')
body('3.爽约由谁触发：Reservation.handleNoShow()，执行后把预约状态从BOOKED直接置为NO_SHOW（而不是CANCELLED），释放座位、扣5分并写一条reason=NO_SHOW的CreditScoreLog。把NO_SHOW从CANCELLED里独立出来，是为了后续能统计爽约率，而不会和学生主动取消混在一起。')

h2('（三）分析类图与设计类图的边界')
body('本类图刻意只画业务实体与枚举，没有画Service、DAO、Factory这些技术分层类。理由是：')
body('1.实验3的指导书要求是「从业务与功能中找出重要的类和对象」，重点是业务静态结构；')
body('2.Service/DAO属于设计阶段的技术决策，过早画进类图会把分析产物和设计产物混在一起；')
body('3.后续A-5（实体与DAO）、A-6（Service业务逻辑）会在编码阶段补出这些类，届时再画设计类图或时序图。')
body('这是一种「分析类图先瘦、设计类图再补」的分层做法，与课程指导书从CIM到PIM的演进路径一致。')

h2('（四）与契约的一致性核对')
body('类图中的7个业务类、4个枚举，其属性名、关键方法名（cancel／checkIn／checkOut／handleNoShow／updateStatus／login／modifyInfo）、枚举取值，逐项对照docs/contract/接口契约.md v0.6与B评审意见，未发现不一致。特别是handleNoShow将预约状态置为NO_SHOW并写CreditScoreLog、SeatStatus.OCCUPIED不可直接改AVAILABLE的状态约束，均已体现在类图与方法说明中。')

# ================= 六、分工 =================
h1('六、分工与评审说明')
table(['姓名', '学号', '本报告承担工作'], [
    ['易雨杰', '2024611209', '名词法分析、类的属性与操作设计、类图在StarUML中手画、类字典撰写、报告排版'],
    ['叶梅冰', '2024611038', '需求复核、类图与类字典评审、与接口契约的一致性检查'],
], widths=[2.2, 3.0, 10.4])
body('本报告由成员A易雨杰主笔撰写，成员B叶梅冰参与评审。')
body('仓库：https://github.com/STU-team/Study_Room_Reservation')

doc.save(OUT)
print('已保存:', OUT)
print('段落数:', len(doc.paragraphs), '表格数:', len(doc.tables))
print('图片已插入:', os.path.exists(IMG))
