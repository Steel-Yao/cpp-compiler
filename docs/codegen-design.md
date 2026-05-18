# C++ 编译器第五步：目标代码生成器设计

## 1. 目标代码生成器的作用

目标代码生成器（Code Generator）的任务，是把前端和中端已经处理好的中间代码，进一步转换为某种更接近真实机器执行形式的目标代码。

在本项目当前的编译流程中，已经具备以下阶段：

1. 词法分析：把源代码转换为 Token 序列
2. 语法分析：根据 LL(1) 文法构造语法分析树
3. 语义分析：执行作用域、类型和声明检查
4. 中间代码生成：生成三地址码风格的四元式
5. 中间代码优化：对四元式进行常量折叠、传播、死代码删除等优化

目标代码生成器位于优化器之后，是编译流程后端的核心部分。

如果说中间代码是“与机器无关的程序表示”，那么目标代码就是“面向某种具体执行模型的程序表示”。例如，四元式：

```text
(+, a, b, t1)
(=, t1, -, x)
```

可以被翻译成类似汇编的目标代码：

```asm
LOAD R1, a
ADD R1, b
STORE x, R1
```

本项目是教学导向的 C++ 子集编译器，因此目标代码生成器可以先选择一种简化的、易读的伪汇编作为目标语言，而不是直接生成真实 x86、ARM 或 JVM 字节码。

---

## 2. 目标代码生成的基本原理

目标代码生成的核心思想，是把中间表示中的每条操作，映射为目标机器或虚拟机器能够执行的一组指令。

通常需要解决以下问题：

1. **指令选择**：某条四元式应该翻译成哪些目标指令
2. **寄存器分配**：中间变量和临时变量应该放在哪些寄存器中
3. **内存管理**：变量何时从内存加载，何时写回内存
4. **控制流翻译**：标签、条件跳转和无条件跳转如何转换
5. **函数调用约定**：参数、返回值和栈帧如何组织

对于完整编译器来说，目标代码生成是一个复杂后端阶段。但在本项目中，可以先采用简化模型：

- 使用少量通用寄存器，如 `R1`、`R2`、`R3`、`R4`
- 所有源语言变量默认存放在内存变量中
- 临时变量可以尽量使用寄存器，也可以在必要时写回临时内存位置
- 控制流直接使用标签和跳转指令表示
- 函数调用和栈帧先作为后续扩展

这样既能展示目标代码生成的主要思想，又不会过早引入真实机器体系结构的复杂细节。

---

## 3. 本项目的目标代码形式

为了便于教学和调试，本项目可以设计一种简化伪汇编作为目标代码。

### 3.1 指令格式

目标指令可以采用统一的文本格式：

```text
OP operand1, operand2
```

或：

```text
OP operand
```

例如：

```asm
LOAD R1, a
ADD R1, b
STORE x, R1
LABEL L1
JZ R1, L2
JMP L3
RET R1
```

这种形式具有几个优点：

- 比真实汇编简单
- 能清晰展示计算过程
- 与四元式容易对应
- 便于在 `Main` 中直接打印演示

### 3.2 基础指令集

第一版目标代码生成器可以支持以下指令：

#### 数据传送指令

```asm
LOAD R, x      ; 从变量或常量加载到寄存器
STORE x, R     ; 将寄存器内容写回变量
MOV R1, R2     ; 寄存器之间复制
```

#### 算术运算指令

```asm
ADD R, x
SUB R, x
MUL R, x
DIV R, x
MOD R, x
NEG R
```

这些指令可以采用二地址形式：

```asm
ADD R1, R2
```

表示：

```text
R1 = R1 + R2
```

#### 关系与逻辑运算指令

```asm
CMP_LT R, x
CMP_LE R, x
CMP_GT R, x
CMP_GE R, x
CMP_EQ R, x
CMP_NE R, x
AND R, x
OR R, x
NOT R
```

比较结果可以约定为：

- 真：`1`
- 假：`0`

#### 控制流指令

```asm
LABEL L1
JMP L1
JZ R, L1
JNZ R, L1
RET R
RET
```

其中：

- `LABEL` 表示标签定义
- `JMP` 表示无条件跳转
- `JZ` 表示寄存器为 0 时跳转
- `JNZ` 表示寄存器非 0 时跳转
- `RET` 表示函数返回

---

## 4. 四元式到目标代码的映射

当前项目的 IR 采用四元式：

```text
(op, arg1, arg2, result)
```

目标代码生成器的核心工作，就是根据 `op` 的不同生成对应指令。

### 4.1 赋值指令

四元式：

```text
(=, 10, -, a)
```

可生成：

```asm
LOAD R1, 10
STORE a, R1
```

如果右侧是变量：

```text
(=, t1, -, a)
```

可生成：

```asm
LOAD R1, t1
STORE a, R1
```

### 4.2 算术运算

四元式：

```text
(+, a, b, t1)
```

可生成：

```asm
LOAD R1, a
ADD R1, b
STORE t1, R1
```

减法、乘法、除法和取模类似：

```text
(-, a, b, t1)  -> SUB
(*, a, b, t1)  -> MUL
(/, a, b, t1)  -> DIV
(%, a, b, t1)  -> MOD
```

### 4.3 一元运算

四元式：

```text
(uminus, a, -, t1)
```

可生成：

```asm
LOAD R1, a
NEG R1
STORE t1, R1
```

四元式：

```text
(!, flag, -, t1)
```

可生成：

```asm
LOAD R1, flag
NOT R1
STORE t1, R1
```

### 4.4 关系运算

四元式：

```text
(GE, a, 10, t1)
```

可生成：

```asm
LOAD R1, a
CMP_GE R1, 10
STORE t1, R1
```

关系运算的目标是生成布尔结果，即 `0` 或 `1`。

常见映射包括：

```text
<   -> CMP_LT
<=  -> CMP_LE
>   -> CMP_GT
>=  -> CMP_GE
==  -> CMP_EQ
!=  -> CMP_NE
LT  -> CMP_LT
LE  -> CMP_LE
GT  -> CMP_GT
GE  -> CMP_GE
EQ  -> CMP_EQ
NEQ -> CMP_NE
```

### 4.5 逻辑运算

四元式：

```text
(&&, t1, t2, t3)
```

可生成：

```asm
LOAD R1, t1
AND R1, t2
STORE t3, R1
```

四元式：

```text
(||, t1, t2, t3)
```

可生成：

```asm
LOAD R1, t1
OR R1, t2
STORE t3, R1
```

第一版可以先把逻辑运算当作普通数值布尔运算处理，不实现短路求值。短路求值可以在语义分析或 IR 生成阶段进一步扩展。

### 4.6 标签与跳转

四元式：

```text
(label, -, -, L1)
```

可生成：

```asm
LABEL L1
```

四元式：

```text
(goto, -, -, L1)
```

可生成：

```asm
JMP L1
```

四元式：

```text
(jz, t1, -, L1)
```

可生成：

```asm
LOAD R1, t1
JZ R1, L1
```

如果后续加入 `jnz`，则可生成：

```asm
LOAD R1, t1
JNZ R1, L1
```

### 4.7 return 指令

四元式：

```text
(return, t1, -, -)
```

可生成：

```asm
LOAD R1, t1
RET R1
```

无返回值的情况：

```text
(return, -, -, -)
```

可生成：

```asm
RET
```

---

## 5. 寄存器分配设计

目标代码生成器需要决定中间值放在哪个寄存器中。

### 5.1 简单寄存器模型

第一版可以假设目标机器有 4 个通用寄存器：

```text
R1, R2, R3, R4
```

其中 `R1` 可以作为默认计算寄存器：

```asm
LOAD R1, a
ADD R1, b
STORE t1, R1
```

这种方式实现简单，但会频繁读写内存。

### 5.2 临时寄存器分配

为了减少不必要的 `LOAD` 和 `STORE`，可以维护一张寄存器描述表：

```text
寄存器 -> 当前保存的变量或临时值
```

例如：

```text
R1 -> t1
R2 -> a
R3 -> b
R4 -> 空
```

当需要使用某个变量时：

1. 如果变量已经在某个寄存器中，直接复用
2. 如果有空闲寄存器，加载到空闲寄存器
3. 如果没有空闲寄存器，选择一个寄存器写回并替换

### 5.3 第一版建议

为了保持目标代码生成器结构清晰，第一版可以先采用“单寄存器翻译”策略：

- 每条计算指令使用 `R1`
- 每次计算后立即 `STORE` 到结果位置
- 不做复杂寄存器复用

这样生成的目标代码可能不是最高效，但非常容易验证。

后续再扩展为多寄存器分配。

---

## 6. 内存与变量管理

当前编译器还没有真正的运行时栈帧、全局数据区或局部变量地址分配。因此，目标代码可以先使用符号名直接作为内存位置。

例如：

```asm
STORE a, R1
LOAD R1, a
```

这里的 `a` 可以理解为一个抽象内存单元。

### 6.1 变量位置

对于源程序变量：

```cpp
int a = 10;
```

目标代码可以直接使用变量名：

```asm
LOAD R1, 10
STORE a, R1
```

### 6.2 临时变量位置

对于 IR 临时变量：

```text
t1, t2, t3
```

第一版可以同样把它们看作临时内存位置：

```asm
STORE t1, R1
LOAD R1, t1
```

虽然这会产生较多访存，但可以保持实现简单。

后续如果加入寄存器分配，则可以尽量避免把所有临时变量写回内存。

---

## 7. 控制流生成设计

控制流是目标代码生成中的关键部分。

### 7.1 if 语句

语义分析和 IR 阶段已经把 `if` 结构转换为标签和条件跳转。

例如：

```text
(GE, a, 10, t1)
(jz, t1, -, L1)
(return, a, -, -)
(goto, -, -, L2)
(label, -, -, L1)
(return, 0, -, -)
(label, -, -, L2)
```

目标代码生成器只需要按顺序翻译：

```asm
LOAD R1, a
CMP_GE R1, 10
STORE t1, R1
LOAD R1, t1
JZ R1, L1
LOAD R1, a
RET R1
JMP L2
LABEL L1
LOAD R1, 0
RET R1
LABEL L2
```

也就是说，复杂语句结构已经在 IR 阶段被展开，目标代码生成器不需要重新理解语法树。

### 7.2 while 语句

`while` 语句同样由标签和跳转表示：

```text
(label, -, -, L1)
(<, i, 10, t1)
(jz, t1, -, L2)
(+, i, 1, t2)
(=, t2, -, i)
(goto, -, -, L1)
(label, -, -, L2)
```

可生成：

```asm
LABEL L1
LOAD R1, i
CMP_LT R1, 10
STORE t1, R1
LOAD R1, t1
JZ R1, L2
LOAD R1, i
ADD R1, 1
STORE t2, R1
LOAD R1, t2
STORE i, R1
JMP L1
LABEL L2
```

这种设计体现了一个重要原则：

> 目标代码生成器面向 IR，而不是面向源语言语法。

---

## 8. Java 模块实现思路

目标代码生成器可以作为独立包实现：

```text
src/main/java/org/yyds/codegen
```

建议包含以下类。

### 8.1 TargetInstruction

`TargetInstruction` 表示一条目标代码指令。

```java
public record TargetInstruction(String op, List<String> operands) {
    @Override
    public String toString() {
        if (operands.isEmpty()) {
            return op;
        }
        return op + " " + String.join(", ", operands);
    }
}
```

例如：

```java
new TargetInstruction("LOAD", List.of("R1", "a"))
```

打印为：

```asm
LOAD R1, a
```

### 8.2 CodeGenerator

`CodeGenerator` 是目标代码生成入口。

```java
public class CodeGenerator {
    public List<TargetInstruction> generate(List<Quadruple> quadruples) {
        List<TargetInstruction> instructions = new ArrayList<>();
        for (Quadruple quadruple : quadruples) {
            generateOne(quadruple, instructions);
        }
        return instructions;
    }
}
```

它的职责包括：

- 遍历优化后的四元式
- 根据 `op` 选择翻译规则
- 生成目标指令列表
- 返回可打印的目标代码

### 8.3 RegisterAllocator

如果需要进一步扩展寄存器分配，可以设计：

```java
public class RegisterAllocator {
    public String acquire(String value) {
        // 返回保存该值的寄存器
    }

    public void release(String register) {
        // 释放寄存器
    }
}
```

第一版可以不单独实现复杂分配器，而是在 `CodeGenerator` 中固定使用 `R1`。

### 8.4 生成入口示例

未来可在 `Main` 中加入：

```java
CodeGenerator codeGenerator = new CodeGenerator();
List<TargetInstruction> targetCode = codeGenerator.generate(optimizedQuadruples);

System.out.println("\n目标代码：");
for (TargetInstruction instruction : targetCode) {
    System.out.println(instruction);
}
```

这样完整演示流程就变为：

```text
Lexer -> Parser -> SemanticAnalyzer -> Optimizer -> CodeGenerator -> 目标代码
```

---

## 9. 完整翻译示例

假设优化后的四元式为：

```text
(=, 10, -, a)
(=, 3.14, -, b)
(return, 11, -, -)
```

目标代码可以生成为：

```asm
LOAD R1, 10
STORE a, R1
LOAD R1, 3.14
STORE b, R1
LOAD R1, 11
RET R1
```

如果没有经过优化，四元式可能是：

```text
(=, 10, -, a)
(+, a, 1, t1)
(return, t1, -, -)
```

则目标代码为：

```asm
LOAD R1, 10
STORE a, R1
LOAD R1, a
ADD R1, 1
STORE t1, R1
LOAD R1, t1
RET R1
```

通过对比可以看出，优化器能够减少目标代码生成器需要处理的冗余计算。

---

## 10. 错误处理与边界情况

目标代码生成器通常假设输入 IR 已经通过语义分析和优化器处理，因此不需要重复执行复杂语义检查。

但它仍然需要处理一些边界情况：

1. **未知操作码**：遇到未支持的 `op` 时应报告明确错误
2. **缺失操作数**：例如算术运算缺少 `arg1` 或 `arg2`
3. **非法跳转目标**：跳转指令没有 `result` 标签
4. **空返回值**：`return` 是否带值应正确区分

可以定义目标代码生成异常：

```java
public class CodeGenerationException extends RuntimeException {
    public CodeGenerationException(String message) {
        super(message);
    }
}
```

错误信息应尽量包含原始四元式，方便定位问题。

---

## 11. 与现有编译流程的衔接

目标代码生成器建议直接使用优化后的四元式作为输入。

当前流程：

```text
Lexer -> Parser -> SemanticAnalyzer -> Optimizer -> 打印优化后 IR
```

加入目标代码生成后：

```text
Lexer -> Parser -> SemanticAnalyzer -> Optimizer -> CodeGenerator -> 打印目标代码
```

这种分层方式有几个优点：

- `SemanticAnalyzer` 只负责语义检查和 IR 生成
- `Optimizer` 只负责改写 IR
- `CodeGenerator` 只负责把 IR 翻译为目标代码
- 每个阶段都可以单独打印和调试

目标代码生成器不需要读取语法树，也不需要访问语义符号表。它只需要理解四元式操作码和操作数即可。

---

## 12. 设计小结

本阶段目标代码生成器的核心目标，是把优化后的四元式 IR 转换为易读的伪汇编目标代码。

第一版可以采用以下简化策略：

- 目标代码使用教学型伪汇编
- 固定使用 `R1` 作为主要计算寄存器
- 源程序变量和临时变量都用符号名表示内存位置
- 每条四元式按规则直接翻译为一组目标指令
- 控制流通过 `LABEL`、`JMP`、`JZ`、`JNZ`、`RET` 表示

这种设计虽然不追求最优目标代码，但非常适合当前项目：

- 容易实现
- 容易调试
- 容易展示编译器后端思想
- 能自然衔接现有 `Quadruple` 和 `Optimizer`

后续可以继续扩展：

- 多寄存器分配
- 栈帧和函数调用约定
- 局部变量地址分配
- 面向真实汇编语言的指令选择
- 目标代码级窥孔优化

对于当前教学型 C++ 子集编译器来说，先实现伪汇编目标代码生成器，就可以形成一条完整的编译演示链路：从源代码到 Token、语法树、IR、优化后 IR，再到最终目标代码。
