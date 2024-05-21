package com.relogiclabs.jschema.function;

import com.relogiclabs.jschema.exception.JsonSchemaException;
import com.relogiclabs.jschema.internal.function.DateTimeAgent;
import com.relogiclabs.jschema.internal.time.DateTimeParser;
import com.relogiclabs.jschema.message.ActualDetail;
import com.relogiclabs.jschema.message.ErrorDetail;
import com.relogiclabs.jschema.message.ExpectedDetail;
import com.relogiclabs.jschema.node.JDateTime;
import com.relogiclabs.jschema.node.JString;
import com.relogiclabs.jschema.node.JUndefined;
import com.relogiclabs.jschema.time.DateTimeType;

import static com.relogiclabs.jschema.message.ErrorCode.AFTR01;
import static com.relogiclabs.jschema.message.ErrorCode.AFTR02;
import static com.relogiclabs.jschema.message.ErrorCode.BFOR01;
import static com.relogiclabs.jschema.message.ErrorCode.BFOR02;
import static com.relogiclabs.jschema.message.ErrorCode.DRNG01;
import static com.relogiclabs.jschema.message.ErrorCode.DRNG02;
import static com.relogiclabs.jschema.message.ErrorCode.DRNG03;
import static com.relogiclabs.jschema.message.ErrorCode.DRNG04;
import static com.relogiclabs.jschema.message.ErrorCode.DRNG05;
import static com.relogiclabs.jschema.message.ErrorCode.DRNG06;
import static com.relogiclabs.jschema.message.ErrorCode.DRNG07;
import static com.relogiclabs.jschema.message.ErrorCode.DRNG08;
import static com.relogiclabs.jschema.message.ErrorCode.ENDE01;
import static com.relogiclabs.jschema.message.ErrorCode.ENDE02;
import static com.relogiclabs.jschema.message.ErrorCode.STRT01;
import static com.relogiclabs.jschema.message.ErrorCode.STRT02;
import static com.relogiclabs.jschema.time.DateTimeType.DATE_TYPE;

public final class CoreFunctions4 extends CoreFunctions3 {
    public boolean date(JString target, JString pattern) {
        return dateTime(target, pattern, DATE_TYPE);
    }

    public boolean time(JString target, JString pattern) {
        return dateTime(target, pattern, DateTimeType.TIME_TYPE);
    }

    private boolean dateTime(JString target, JString pattern, DateTimeType type) {
        return new DateTimeAgent(pattern.getValue(), type).parse(caller, target) != null;
    }

    public boolean before(JDateTime target, JString reference) {
        var dateTimeNode = getDateTime(target.getDateTimeParser(), reference);
        if(dateTimeNode == null) return false;
        if(target.getDateTime().compare(dateTimeNode.getDateTime()) < 0) return true;
        var dateTime = target.getDateTime().getType();
        var code = dateTime == DATE_TYPE ? BFOR01 : BFOR02;
        return fail(new JsonSchemaException(
            new ErrorDetail(code, dateTime + " is not earlier than specified"),
            new ExpectedDetail(caller, "a " + dateTime + " before " + reference),
            new ActualDetail(target, "found " + target + " which is not inside limit")
        ));
    }

    public boolean after(JDateTime target, JString reference) {
        var dateTimeNode = getDateTime(target.getDateTimeParser(), reference);
        if(dateTimeNode == null) return false;
        if(target.getDateTime().compare(dateTimeNode.getDateTime()) > 0) return true;
        var dateTime = target.getDateTime().getType();
        var code = dateTime == DATE_TYPE ? AFTR01 : AFTR02;
        return fail(new JsonSchemaException(
            new ErrorDetail(code, dateTime + " is not later than specified"),
            new ExpectedDetail(caller, "a " + dateTime + " after " + reference),
            new ActualDetail(target, "found " + target + " which is not inside limit")
        ));
    }

    public boolean range(JDateTime target, JString start, JString end) {
        var rStart = getDateTime(target.getDateTimeParser(), start);
        if(rStart == null) return false;
        var rEnd = getDateTime(target.getDateTimeParser(), end);
        if(rEnd == null) return false;
        if(target.getDateTime().compare(rStart.getDateTime()) < 0)
            return failOnStartDate(target, rStart, getErrorCode(target, DRNG01, DRNG02));
        if(target.getDateTime().compare(rEnd.getDateTime()) > 0)
            return failOnEndDate(target, rEnd, getErrorCode(target, DRNG03, DRNG04));
        return true;
    }

    private static String getErrorCode(JDateTime target, String date, String time) {
        return target.getDateTime().getType() == DATE_TYPE ? date : time;
    }

    private boolean failOnStartDate(JDateTime target, JDateTime start, String code) {
        var dateTime = target.getDateTime().getType();
        return fail(new JsonSchemaException(
            new ErrorDetail(code, dateTime + " is earlier than start " + dateTime),
            new ExpectedDetail(caller, "a " + dateTime + " from or after " + start),
            new ActualDetail(target, "found " + target + " which is before start " + dateTime)
        ));
    }

    private boolean failOnEndDate(JDateTime target, JDateTime end, String code) {
        var dateTime = target.getDateTime().getType();
        return fail(new JsonSchemaException(
            new ErrorDetail(code, dateTime + " is later than end " + dateTime),
            new ExpectedDetail(caller, "a " + dateTime + " until or before " + end),
            new ActualDetail(target, "found " + target + " which is after end " + dateTime)
        ));
    }

    public boolean range(JDateTime target, JUndefined start, JString end) {
        var rEnd = getDateTime(target.getDateTimeParser(), end);
        if(rEnd == null) return false;
        if (target.getDateTime().compare(rEnd.getDateTime()) <= 0) return true;
        return failOnEndDate(target, rEnd, getErrorCode(target, DRNG05, DRNG06));
    }

    public boolean range(JDateTime target, JString start, JUndefined end) {
        var rStart = getDateTime(target.getDateTimeParser(), start);
        if(rStart == null) return false;
        if (target.getDateTime().compare(rStart.getDateTime()) >= 0) return true;
        return failOnStartDate(target, rStart, getErrorCode(target, DRNG07, DRNG08));
    }

    public boolean start(JDateTime target, JString reference) {
        var dateTimeNode = getDateTime(target.getDateTimeParser(), reference);
        if(dateTimeNode == null) return false;
        if(target.getDateTime().compare(dateTimeNode.getDateTime()) < 0) {
            var dateTime = target.getDateTime().getType();
            var code = dateTime == DATE_TYPE ? STRT01 : STRT02;
            return fail(new JsonSchemaException(
                new ErrorDetail(code, dateTime + " is earlier than specified"),
                new ExpectedDetail(caller, "a " + dateTime + " from or after " + dateTimeNode),
                new ActualDetail(target, "found " + target + " which is before limit")
            ));
        }
        return true;
    }

    public boolean end(JDateTime target, JString reference) {
        var dateTimeNode = getDateTime(target.getDateTimeParser(), reference);
        if(dateTimeNode == null) return false;
        if(target.getDateTime().compare(dateTimeNode.getDateTime()) > 0) {
            var dateTime = target.getDateTime().getType();
            var code = dateTime == DATE_TYPE ? ENDE01 : ENDE02;
            return fail(new JsonSchemaException(
                new ErrorDetail(code, dateTime + " is later than specified"),
                new ExpectedDetail(caller, "a " + dateTime + " until or before " + dateTimeNode),
                new ActualDetail(target, "found " + target + " which is after limit")
            ));
        }
        return true;
    }

    private JDateTime getDateTime(DateTimeParser parser, JString dateTime) {
        if(dateTime.getDerived() instanceof JDateTime result
            && result.getDateTime().getType() == parser.getType()) return result;
        var jDateTime = new DateTimeAgent(parser).parse(caller, dateTime);
        if(jDateTime == null) return null;
        dateTime.setDerived(jDateTime.createNode(dateTime));
        return (JDateTime) dateTime.getDerived();
    }
}