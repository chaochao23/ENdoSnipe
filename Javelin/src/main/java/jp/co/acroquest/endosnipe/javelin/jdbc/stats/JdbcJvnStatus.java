/*******************************************************************************
 * ENdoSnipe 5.0 - (https://github.com/endosnipe)
 * 
 * The MIT License (MIT)
 * 
 * Copyright (c) 2012 Acroquest Technology Co.,Ltd.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package jp.co.acroquest.endosnipe.javelin.jdbc.stats;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import jp.co.acroquest.endosnipe.javelin.CallTreeRecorder;
import jp.co.acroquest.endosnipe.javelin.util.HashMap;
import jp.co.acroquest.endosnipe.javelin.util.HashSet;

/**
 * JDBCJavelin���X���b�h���Ɏ���Ԓl�ł��B
 * 
 * @author eriguchi
 */
public class JdbcJvnStatus
{
    /**
     * ���s�v��擾���ɁA����p��Statement��o�^����t�B�[���h�B
     * ���s�v��擾���łȂ����null��ݒ肵�Ă����܂��B
     */
    private Statement nowExpalaining_;

    /** ���\�b�h�Ăяo���̐[�� */
    private int depth_;

    /** ���\�b�h�Ăяo���̐[���̍ő�l */
    private int depthMax_;

    /** callDepth_���L�[�Ƃ��ASQL���ۑ�����Ă��Ȃ����ǂ�����l�Ƃ���}�b�v�B */
    private Map<Integer, Boolean> noSqlArgsMap_;

    /** JdbcJavelinRecorder�̌Ăяo���̐[���B  */
    private int callDepth_;

    /** SQL�̈ꎞ�ۑ��� */
    private String[] execPlanSql_;

    /** Preprocess�����[���B  */
    private Set<Integer> preprocessedDepthSet_;
    
    /** �R�[���c���[���R�[�_ */
    private CallTreeRecorder callTreeRecorder_;

    /**
     * ���s�v��擾���ɁA����p��Statement��o�^����t�B�[���h�B
     * ���s�v��擾���łȂ����null��ݒ肵�Ă����܂��B
     */
    private Connection nowCalling_;

    /** JDBC Javelin���X���b�h���Ɏ���ԁB */
    private static ThreadLocal<JdbcJvnStatus> jdbcJvnStatus__ = new ThreadLocal<JdbcJvnStatus>() {
        @Override
        protected synchronized JdbcJvnStatus initialValue()
        {
            return new JdbcJvnStatus();
        }
    };

    /**
     * �R���X�g���N�^�B
     */
    private JdbcJvnStatus()
    {
        this.nowExpalaining_ = null;
        this.depth_ = 0;
        this.depthMax_ = 0;
        this.noSqlArgsMap_ = new HashMap<Integer, Boolean>();
        this.callDepth_ = 0;
        this.execPlanSql_ = null;
        this.preprocessedDepthSet_ = new HashSet<Integer>();
        this.nowCalling_ = null;
    }

    /**
     * �R�[���c���[���R�[�_���擾����B
     * 
     * @return �R�[���c���[���R�[�_
     */
    public CallTreeRecorder getCallTreeRecorder()
    {
        if (this.callTreeRecorder_ == null)
        {
            this.callTreeRecorder_ = CallTreeRecorder.getInstance();
        }

        return callTreeRecorder_;
    }

    /**
     * �Y������X���b�h�ɑ�����C���X�^���X���擾���܂��B
     * 
     * @return �C���X�^���X�B
     */
    public static JdbcJvnStatus getInstance()
    {
        return jdbcJvnStatus__.get();
    }

    /**
     * �[����1���₵�܂��B
     * 
     * @return �X�V��̐[���B
     */
    public int incrementDepth()
    {
        this.depth_++;
        this.depthMax_ =  this.depth_;
        return this.depth_;
    }

    /**
     * JdbcJavelinRecorder�̌Ăяo���̐[�����C���N�������g���܂��B
     */
    public void incrementCallDepth()
    {
        this.callDepth_++;
    }

    /**
     * JdbcJavelinRecorder�̌Ăяo���̐[�����f�N�������g���܂��B
     */
    public void decrementCallDepth()
    {
        this.callDepth_--;
    }

    /**
     * ���O�������s�������ǂ������擾���܂��B
     * 
     * @return ���O�������s�������ǂ����B
     */
    public boolean isPreprocessDepth()
    {
        Set<Integer> preprocessedDepthSet = this.preprocessedDepthSet_;
        if (preprocessedDepthSet.isEmpty() == true)
        {
            return false;
        }
        return preprocessedDepthSet.contains(this.callDepth_);
    }

    /**
     * ���O�������s�������ǂ����̃t���O���A�N���A���܂��B
     */
    public void removePreProcessDepth()
    {
        this.preprocessedDepthSet_.remove(this.callDepth_);
    }

    /**
     * ThreadLocal�Ɏ��s�v��擾�p��SQL���i�[����.
     * @param sql �ꎞ�ۑ�����SQL
     */
    public void setExecPlanSql(final Object[] sql)
    {

        // ���ɐݒ肳��Ă���ꍇ�͉������Ȃ��B
        String[] execPlanSql = this.execPlanSql_;
        if (execPlanSql != null && execPlanSql.length > 0)
        {
            return;
        }

        if (sql == null || sql.length == 0)
        {
            this.setExecPlanSql(null);
            return;
        }
        String[] strSql = new String[sql.length];
        System.arraycopy(sql, 0, strSql, 0, sql.length);
        this.setExecPlanSql(strSql);
    }

    public void setNoSql(Boolean noSql)
    {
        this.noSqlArgsMap_.put(this.callDepth_, noSql);
    }

    public void savePreprocessDepth()
    {
        this.preprocessedDepthSet_.add(this.callDepth_);
    }

    public Boolean isNoSql()
    {
        return this.noSqlArgsMap_.get(this.callDepth_);
    }

    public void decrementDepth()
    {
        this.depth_--;
    }

    public void clearPreprocessedDepthSet()
    {
        this.preprocessedDepthSet_.clear();
    }

    public Statement getNowExpalaining()
    {
        return nowExpalaining_;
    }

    public void setNowExpalaining(Statement nowExpalaining)
    {
        nowExpalaining_ = nowExpalaining;
    }

    public int getDepth()
    {
        return depth_;
    }

    public void setDepth(int depth)
    {
        depth_ = depth;
    }

    public int getDepthMax()
    {
        return depthMax_;
    }

    public void setDepthMax(int depthMax)
    {
        depthMax_ = depthMax;
    }

    public int getCallDepth()
    {
        return callDepth_;
    }

    public void setCallDepth(int callDepth)
    {
        callDepth_ = callDepth;
    }

    public String[] getExecPlanSql()
    {
        return execPlanSql_;
    }

    public void setExecPlanSql(String[] execPlanSql)
    {
        execPlanSql_ = execPlanSql;
    }

    public Connection getNowCalling_()
    {
        return nowCalling_;
    }

    public void setNowCalling_(Connection nowCalling)
    {
        this.nowCalling_ = nowCalling;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append("[nowExplaining=");
        builder.append(this.nowExpalaining_);
        builder.append(", depth=");
        builder.append(this.depth_);
        builder.append(", depthMax=");
        builder.append(this.depthMax_);
        builder.append(", noSqlArgsMap=");
        builder.append(this.noSqlArgsMap_);
        builder.append(", callDepth=");
        builder.append(this.callDepth_);
        builder.append(", execPlanSql=");
        builder.append(Arrays.toString(this.execPlanSql_));
        builder.append(", preprocessedDepthSet=");
        builder.append(this.preprocessedDepthSet_);
        if (this.getCallTreeRecorder() != null)
        {
            builder.append(", callerNode=");
            builder.append(this.getCallTreeRecorder().getCallTreeNode());
        }
        builder.append(", callTreeRecorder=");
        builder.append(this.callTreeRecorder_);
        builder.append("]");
        return builder.toString();
    }
}