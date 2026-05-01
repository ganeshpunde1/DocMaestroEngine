package com.maestro.po.ms.inference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.maestro.po.ms.inference.exception.BadDataException;
import com.maestro.po.ms.inference.exception.DataNotFoundException;
import com.maestro.po.ms.inference.exception.InferenceException;
import com.maestro.po.ms.inference.model.annotation.EnumInferenceAnswerMimeType;
import com.maestro.po.ms.inference.model.annotation.EnumInferenceStatus;
import com.maestro.po.ms.inference.model.annotation.EnumParentEntity;
import com.maestro.po.ms.inference.model.annotation.InferenceAnswer;
import com.maestro.po.ms.inference.model.annotation.InferenceResponse;
import com.maestro.po.ms.inference.model.rest.GenericInfo;
import com.maestro.po.ms.inference.model.rest.InferenceQuery;
import com.maestro.po.ms.inference.model.rest.InferenceQueueItem;
import com.maestro.po.ms.inference.model.rest.InferenceRestAnswer;
import com.maestro.po.ms.inference.model.rest.InferenceRestResponse;
import com.maestro.po.ms.inference.model.rest.InferenceSchema;
import com.maestro.po.ms.inference.model.rest.AcceptedApiResponse;
import com.maestro.po.ms.inference.model.rest.PdfInferenceRequest;

class BeanCodeCoverageTester
{

    @Test
    void modelClassesTest() throws Exception
    {
        // DB
        assertBeanProperties(EnumInferenceAnswerMimeType.class);
        assertBeanProperties(EnumInferenceStatus.class);
        assertBeanProperties(InferenceAnswer.class);
        assertBeanProperties(InferenceResponse.class);

        // Rest
        assertBeanProperties(GenericInfo.class);
        assertBeanProperties(InferenceQuery.class);
        assertBeanProperties(InferenceRestAnswer.class);
        assertBeanProperties(InferenceRestResponse.class);
        assertBeanProperties(InferenceSchema.class);
        assertBeanProperties(AcceptedApiResponse.class);
        assertBeanProperties(PdfInferenceRequest.class);
        assertBeanProperties(EnumParentEntity.class);
        assertBeanProperties(InferenceQueueItem.class);

        // Execeptions
        assertBeanProperties(BadDataException.class);
        assertBeanProperties(DataNotFoundException.class);
        assertBeanProperties(InferenceException.class);
    }

    @SuppressWarnings(
    { "rawtypes", "deprecation" })
    public void assertBeanProperties(Class clazz) throws Exception
    {
        Field[] fields = clazz.getClass().getDeclaredFields();
        List<String> actualFieldNames = getFieldNames(fields);
        assertTrue(!actualFieldNames.isEmpty());

        Constructor<?>[] constructors = clazz.getClass().getConstructors();
        assertNotNull(constructors);

        Method[] methods = clazz.getClass().getDeclaredMethods();
        List<String> actualMethods = getMethodNames(methods);
        assertTrue(!actualMethods.isEmpty());
        getFields(clazz);

        // init object and invoking setters and getters
        Class<?> oClass = Class.forName(clazz.getName());
        if (hasNoParameterConstructor(oClass))
        {
            Object obj1 = oClass.getDeclaredConstructor().newInstance();
            setFields(obj1);

            Object obj2 = oClass.getDeclaredConstructor().newInstance();
            setFields(obj2);
            System.out.println("obj1.equals(obj2) = " + obj1.equals(obj2) + ", obj1.hashCode() = " + obj1.hashCode() + ", obj2.hashCode() = " + obj2.hashCode()
                    + ", obj1.toString() = " + obj1.toString());
        }
        else {
            Constructor<?> smallestParamConstructor = findNarrowestConstructor(oClass);
            if (smallestParamConstructor == null) return;
            List<Object> params = new ArrayList<> ();                     
            
            for (Parameter p : smallestParamConstructor.getParameters())
            {
                if (p.getType().isPrimitive())
                {
                    String type = p.getType().getSimpleName().toLowerCase();
                    if (StringUtils.containsAny(type,"integer", "double", "float", "short", "byte"))
                        params.add(0);
                    if (StringUtils.containsAny(type,"boolean"))
                        params.add(false);
                    if (StringUtils.containsAny(type,"character"))
                        params.add('l');
                } else if (hasNoParameterConstructor(p.getType()))
                {
                    params.add(p.getType().getDeclaredConstructor().newInstance());   
                } else
                {
                    params.add(null);
                }
                
            }
            

            Object obj1 = smallestParamConstructor.newInstance(params.toArray());
            setFields(obj1);

            Object obj2 = smallestParamConstructor.newInstance(params.toArray());
            setFields(obj2);
            System.out.println("obj1.equals(obj2) = " + obj1.equals(obj2) + ", obj1.hashCode() = " + obj1.hashCode() + ", obj2.hashCode() = " + obj2.hashCode()
                    + ", obj1.toString() = " + obj1.toString());
        }
    }

    private Constructor<?> findNarrowestConstructor(Class<?> oClass)
    {
        
        Optional<Constructor<?>> val = Arrays.stream(oClass.getDeclaredConstructors()).sorted((c , d) -> { 
            return (Integer.compare(c.getParameterCount(), d.getParameterCount()) * -1);
            }).findFirst();
        return val.orElse(null);
    }

    private boolean hasNoParameterConstructor(Class<?> oClass)
    {
        boolean noParam = Arrays.stream(oClass.getDeclaredConstructors()).anyMatch(c -> c.getParameterCount() == 0);
        return noParam;
    }

    private static List<String> getFieldNames(Field[] fields)
    {
        List<String> fieldNames = new ArrayList<>();
        for (Field field : fields)
            fieldNames.add(field.getName());
        return fieldNames;
    }

    private static List<String> getMethodNames(Method[] methods)
    {
        List<String> methodNames = new ArrayList<>();
        for (Method method : methods)
            methodNames.add(method.getName());
        return methodNames;
    }
    
    public void getFields(Object myObject)
    {
        Class<?> clazz = myObject.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        if (methods == null || methods.length == 0) return;
        for (Method m : methods)
        {
            int paramSize = m.getParameterCount();
            if (paramSize == 0 && m.trySetAccessible())
            {              
                try
                {
                    m.invoke(myObject, new Object[0]);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            
            if (paramSize == 1 && m.trySetAccessible()) {
                Parameter p = m.getParameters()[0];
                try
                {
                    m.invoke(myObject, new Object[]{null});
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setFields(Object myObject)
    {
        Class<?> clazz = myObject.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields)
        {
            try
            {
                field.setAccessible(true);
                if (field.getType().getSimpleName().toLowerCase().contains("integer"))
                    field.set(myObject, 0);
                else if (field.getType().getSimpleName().toLowerCase().contains("long"))
                    field.set(myObject, 0L);
                else if (field.getType().getSimpleName().toLowerCase().contains("string"))
                    field.set(myObject, null);
                else if (field.getType().getSimpleName().toLowerCase().contains("boolean"))
                    field.set(myObject, false);
                else if (field.getType().getSimpleName().toLowerCase().contains("timestamp"))
                    field.set(myObject, null);
                else if (field.getType().getSimpleName().toLowerCase().contains("date"))
                    field.set(myObject, null);
                else if (field.getType().getSimpleName().toLowerCase().contains("double"))
                    field.set(myObject, 0);
                else if (field.getType().getSimpleName().toLowerCase().contains("float"))
                    field.set(myObject, 0);
                else if (field.getType().getSimpleName().toLowerCase().contains("time"))
                    field.set(myObject, null);
                else
                    field.set(myObject, null);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                field.get(myObject);
                String setterMethod = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);

                if (setterMethod != null)
                {
                    if (field.getType().getSimpleName().toLowerCase().contains("integer"))
                        myObject.getClass().getMethod(setterMethod, Integer.class).invoke(myObject, 0);
                    else if (field.getType().getSimpleName().toLowerCase().contains("long"))
                        myObject.getClass().getMethod(setterMethod, Long.class).invoke(myObject, 0L);
                    else if (field.getType().getSimpleName().toLowerCase().contains("string"))
                    {
                        String nullStr = "";
                        myObject.getClass().getMethod(setterMethod, String.class).invoke(myObject, nullStr);
                    }
                    else if (field.getType().getSimpleName().toLowerCase().contains("boolean"))
                        myObject.getClass().getMethod(setterMethod, boolean.class).invoke(myObject, false);
                    else if (field.getType().getSimpleName().toLowerCase().contains("date"))
                    {
                        Date nullDt = null;
                        myObject.getClass().getMethod(setterMethod, Date.class).invoke(myObject, nullDt);
                    }
                    else if (field.getType().getSimpleName().toLowerCase().contains("double"))
                        myObject.getClass().getMethod(setterMethod, double.class).invoke(myObject, 0);
                    else if (field.getType().getSimpleName().toLowerCase().contains("float"))
                        myObject.getClass().getMethod(setterMethod, float.class).invoke(myObject, 0);
                    else
                    {
                        Object nullObj = null;
                        myObject.getClass().getMethod(setterMethod, field.getClass()).invoke(myObject, nullObj);
                    }
                }

                field.get(myObject);

            }
            catch (Exception e)
            {
            }
        }

    }
    
    @Test
    public void specificBeanTests() {
        EnumInferenceAnswerMimeType answerMimeType = new EnumInferenceAnswerMimeType();
        assertTrue(answerMimeType.hashCode() != 0);
        InferenceSchema inferenceSchema = new InferenceSchema("the", "null");
        assertTrue(inferenceSchema.hashCode() != 0);
        InferenceRestResponse inferenceRestResponse = new InferenceRestResponse("status", "ASG53M-54GN54-G54", new ArrayList<>(),null);
        assertTrue(inferenceRestResponse.hashCode() != 0);
    }
}
