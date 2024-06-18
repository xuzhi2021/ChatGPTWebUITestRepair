import os
import time
import openai
import openai
# from tiktoken import Tokenizer
import json
import hashlib
import shutil
import re


def process_file(path,to_replace,method,sleep_time):
    repair_num = 1
    history = ''
    asked_po=[]
    for root, dirs, files in os.walk(path):
        for filename in files:
            if "po" in root and ".txt" in filename :
                filenames=filename.split("_")
                current=method+"_"+filenames[0]+"_"+filenames[1].split(".")[0]
                print(current)
                print(asked_po)
                if current in asked_po:
                    print("yes, skip "+filename)
                    continue
                else:
                    asked_po.append(current)

            if filename.endswith("all.txt"):
                print("asking")

                filepath = os.path.join(root, filename)
                with open(filepath, 'r',encoding="UTF-8") as f:
                    data = f.read()
                target, candidates=getElementInfo(data)

                to_replace0=to_replace+"/"+method #TODO
                broken_stm=getBrokenStatement(to_replace0,filepath)
                new_filename = filename.replace("all.txt", "answer" + ".txt")
                for i in range(0,4):
                    new_path = os.path.join(root.replace(to_replace, to_replace + "_answers"+str(i)), new_filename)
                    prompt_filename = filename.replace("all.txt", "prompt" + ".txt")
                    prompt_path = os.path.join(root.replace(to_replace, to_replace + "_prompts"+str(i)), prompt_filename)
                    print(new_path)
                    if os.path.exists(new_path):
                        print(new_path + " already exists")
                        continue

                    if "{" not in candidates:
                        print("no candidate for matching, skip")
                        continue
                    print("asking")
                    current_timestamp = time.time()
                    local_time = time.localtime(current_timestamp)
                    formatted_time = time.strftime("%Y-%m-%d %H:%M:%S", local_time)
                    print("Current Time:", formatted_time)
                    content_store,selected_element,prompts,sleep_time=ask(target,candidates,broken_stm,sleep_time)

                    history+="target element"+str(repair_num)+": "+target+","
                    history+="selected element"+str(repair_num)+": "+selected_element+"\r\n"
                    os.makedirs(os.path.dirname(new_path), exist_ok=True)
                    with open(new_path, 'w', encoding="UTF-8") as f:
                        f.write(content_store)

                    os.makedirs(os.path.dirname(prompt_path), exist_ok=True)
                    with open(prompt_path, 'w', encoding="UTF-8") as f:
                        f.write(prompts)
                    print(content_store)

                content_store = ''

def getSelectedElement(id,candidates):
    elements=candidates.split("}")
    target_str="numericId="+str(id)+","
    re=''
    for e in elements:
        if target_str in e:
            re=e
            break
    return e


def getBrokenStatement(to_replace,filepath):

    print("to replace: "+ to_replace)
    p1 = filepath.replace(to_replace, "broken_statement")
    p2 = p1.replace("/", "\\")
    p2 = p2.replace("_all.txt", ".txt")

    print(p2)
    with open(p2, 'r', encoding="UTF-8") as f:
        stm = f.read()
    print("broken statement is: "+stm)
    return stm


def getElementInfo(data):
    prefix1="Target element: "
    index1=data.find(prefix1)+len(prefix1)
    # print(index1)
    prefix2="Candidate elements: "
    index2=data.find(prefix2)
    # print(index2)
    target=data[index1:index2]
    prefix3="Time: "
    index3 = data.find(prefix3)
    # print(index3)
    candidates=data[index2+len(prefix2):index3]
    return target,candidates

def getElementById(candidates,id):
    elements = re.findall(r'\{.*?\}', candidates)
    target_str="numericId="+str(id)
    result=''
    for e in elements:
        if target_str in e:
            result=e
            break
    return result

def countToken(prompt):
    response = openai.Tokencounts.create(prompt=prompt, engine="davinci")# gpt-3.5 turbo is based on davinci
    token_count = response.count
    print("Prompt's token number':", token_count)
    max_token=4096
    return max_token-token_count

def parseMatchResult(data):
    try:
        index1=data.index(".")
        sub_str=data[:index1]
        index2=data.index("numericId: ")
        sub_str2 = sub_str[index2:]
    except ValueError:
        return ''
    re = ''
    for i in range(0, len(sub_str2)):
        if sub_str2[i].isdigit():
            while sub_str2[i].isdigit():
                re += sub_str2[i]
                i += 1
                if i == len(sub_str2):
                    break
            break
    print("parsed selected numericId: "+str(re))
    return re


def find_index(data,str):
    try:
        index=data.index(str)
    except ValueError:
        index=10000
    sub=data[:index]
    if "is" in sub and any(c.isdigit() for c in sub):
        return index
    if "is" in str:
        sub=data[index:]
        if any(c.isdigit() for c in sub):
            return index
    else:
        return 10000

def find_index2(data,str):
    try:
        index=data.index(str)
        # print(index)
    except ValueError:
        index=10001
    # print("index is ")
    # print(index)
    return index


def ask(target, candidates, broken_statement, sleep_time):
    time.sleep(sleep_time)
    #selection_content,selected_element,selection_time,cur_history,prompt1,sleep_time=askSelection(target, candidates, broken_statement, history,sleep_time) #with history
    #without history
    selection_content, selected_element, selection_time, prompt1, sleep_time, invalid= askSelectionWithoutHistory(target,candidates,sleep_time)
    if invalid:
        #the result may be the same as the last repair, so we don't ask ChatGPT to repair again
        store_answers = selection_content + "\n" +  "selection time: " + str(selection_time)
        prompts = "Prompt of selection:" + "\n" + str(prompt1)
        print("return before repair because selection answer is invalid")
        return store_answers, '', prompts, sleep_time
    time.sleep(sleep_time)
    repair_content,repair_time,prompt2,sleep_time=askRepair(selected_element,broken_statement,sleep_time)
    store_answers=selection_content+"\n"+repair_content+"\n"+"selection time: "+\
                  str(selection_time)+"\n"+"repair time: "+str(repair_time)
    prompts="Prompt of selection:"+"\n"+str(prompt1)+"\n\n"+"Prompt of repair:"+"\n"+str(prompt2)
    return store_answers,selected_element,prompts,sleep_time

def askSelectionWithoutHistory(target, candidates, sleep_time):
    reply = ''
    # instruction1="You are a web UI test script repair tool. " \
    #              "To repair the broken statement, you need to choose the element most similar to the target element from the given candidate element list " \
    #              "and give explanations by listing similar attributes."
    instruction1 = "You are a web UI test script repair tool. " \
                   "To repair the broken statement, you need to choose the element most similar to the target element from the given candidate element list firstly. " \
                   "Give me your selected element's numericId and a brief explanation containing the attributes that are most similar to the target element. " \
                   "Your answer should follow the format of this example: " \
                   "\"The most similar element's numericId: 1. Because they share the most similar attributes: id, xpath, text.\""
    history_instruction1 = "Here is your history result: "
    history_instruction2 = "Here are your history results: "

    instruction0 = ''
    user_prompt1 = "Target element: " + target + "\r\n" + "Candidate elements: " + candidates
    cur_history = ''
    count=0
    invalid=False
    # while reply=='':
    while True:
        try:
            start1 = time.time()

            messages1 = [
                {"role": "system", "content": instruction1},
                {"role": "user", "content": user_prompt1},
                {"role": "assistant", "content": 'selected element\'s numeric id:'},
            ]
            messages2 = [
                {"role": "system", "content": instruction1},
                {"role": "user", "content": user_prompt1},
                {"role": "assistant", "content": ''},
                # optional
            ]


            reply = openai.ChatCompletion.create(
                model="gpt-3.5-turbo",
                messages=messages2,
                # max_token=token_limit,
                temperature=0.8  # control the stability of answer
            )
            match_result = reply.choices[0].message["content"]
            id = parseMatchResult(match_result)
            valid_result = True
            if id=='':
                print(match_result)
                print("fail to parse selected element id")
                valid_result = False
            # for i in id:
            #     if not i.isdigit():
            #         valid_result = False
            #         break
            if not valid_result:
                count += 1
                if count == 3:
                    invalid = True
                    end1 = time.time()
                    return reply.choices[0].message["content"], '', end1 - start1, messages2, sleep_time, invalid
                continue  # ask chatgpt again
            selected_element = getElementById(candidates, id)
            if selected_element == '':
                print("fail to parse selected element according to selected numericId")
                valid_result = False
            if not valid_result:
                print(match_result)
                print("parse selected numericId: " + id)
                print("parse selected element's info: " + selected_element)
                print("Get invalid result from ChatGPT, selection is not from candidates.")
                time.sleep(sleep_time)
                count += 1
                if count == 3:
                    invalid = True
                    end1 = time.time()
                    return reply.choices[0].message["content"], '', end1 - start1, messages2, sleep_time, invalid
                print("Ask for matching again.")
                continue
            else:
                print("valid result")
                break
        except openai.error.Timeout as error:
            time.sleep(sleep_time)
            continue
        except openai.error.InvalidRequestError as error:
            print(error)
            print("-----")
            print(messages2)
            print("-----")
            print("Your messages exceeds the limitaion. Reduce the token of your prompt...")
            cur_history = ''

            return
        except openai.error.RateLimitError as error:
            print(f"Rate limit error: {error}")
            print("Waiting for 20 seconds before retrying...")
            if sleep_time < 20:
                sleep_time = 20
                time.sleep(sleep_time)
            else:
                sleep_time = 433
                time.sleep(sleep_time)  # wait for 20 seconds before retrying
        except openai.error.ServiceUnavailableError as error:
            print("openai.error.ServiceUnavailableError: The server is overloaded or not ready yet.")
            time.sleep(sleep_time)
    end1 = time.time()

    match_result = reply.choices[0].message["content"]

    runtime = end1 - start1
    return reply.choices[0].message["content"], selected_element, runtime, messages2, sleep_time, invalid


def askSelection(target, candidates, broken_statement, history,sleep_time):
    reply=''

    instruction1 = "You are a web UI test script repair tool. " \
                   "To repair the broken statement, you need to choose the element most similar to the target element from the given candidate element list " \
                   "and give the most similar attribute names as the format of this example: " \
                   "\"the most similar element\'s numericId: 1. Because they share the most similar attributes: id, xpath, text.\"."
    history_instruction1 = "Here is your history result: "
    history_instruction2 = "Here are your history results: "

    instruction0=''
    user_prompt1="Target element: "+target+"\r\n"+"Candidate elements: "+candidates
    cur_history=history


    # while reply=='':
    while True:
        try:
            if len(history) == 0:
                instruction0 = instruction1  # no history
            elif "target element2" not in history:  # only one pair of result in history
                instruction0 = instruction1 + history_instruction1 + cur_history
            else:
                instruction0 = instruction1 + history_instruction2 + cur_history
            # token_limit=countToken(instruction0+user_prompt1+instruction2+user_prompt2)
            messages1 = [
                {"role": "system", "content": instruction0},
                {"role": "user", "content": user_prompt1},
                {"role": "assistant", "content": 'selected element\'s numeric id:'},
            ]
            messages2 = [
                {"role": "system", "content": instruction0},
                {"role": "user", "content": user_prompt1},
                {"role": "assistant", "content": ''},
                # optional
            ]

            start1 = time.time()
            reply=openai.ChatCompletion.create(
                model="gpt-3.5-turbo",
                messages=messages2,
                # max_token=token_limit,
                temperature=0.8 #control the stability of answer
            )
            match_result = reply.choices[0].message["content"]
            id=parseMatchResult(match_result)
            valid_result=True
            for i in id:
                if not i.isdigit():
                    valid_result=False
                    break
            if not valid_result:
                continue # ask chatgpt again
            selected_element=getElementById(candidates,id)
            if selected_element=='':
                valid_result=False
            if not valid_result:
                print(match_result)
                print("Get invalid result from ChatGPT. Ask again.")
                time.sleep(sleep_time)
                continue
            else:
                break
        except openai.error.InvalidRequestError as error:
            print(error)
            print("-----")
            print(messages2)
            print("-----")
            print("Your messages exceeds the limitaion. Clean the history and ask again...")
            cur_history=''

            # return
        except openai.error.RateLimitError as error:
            print(f"Rate limit error: {error}")
            print("Waiting for 20 seconds before retrying...")
            if sleep_time < 20:
                sleep_time = 20
                time.sleep(sleep_time)
            else:
                sleep_time = 433
                time.sleep(sleep_time)  # wait for 20 seconds before retrying
        except openai.error.ServiceUnavailableError as error:
            print("openai.error.ServiceUnavailableError: The server is overloaded or not ready yet.")
            time.sleep(sleep_time)

    end1=time.time()
    # for choice in reply.choices:
    #     content = choice.message["content"]
    #     print(content)

    # print("-----")
    # print(reply.choices[0].message["content"])
    # print("runtime:\r\n"+str(end1-start1)+"\r\n"+"------------------------------------------------")

    # selected_id=reply.choices[2].message['content']
    # explanation=reply.choices[4].message['content']
    # repaired_Stm=reply.choices[7].message['content']
    # return selected_id,explanation,repaired_Stm,end1-start1
    # return selected_id+"\r\n"+explanation+"\r\n"+repaired_Stm+"\r\n"+"runtime:"+str(end1-start1)+"\r\n"
    # return reply.choices[0].message["content"]+"\r\n"+"runtime:\r\n"+str(end1-start1)+"\r\n"
    match_result=reply.choices[0].message["content"]

    runtime=end1-start1
    return reply.choices[0].message["content"], selected_element,runtime,cur_history,messages2,sleep_time

def askRepair(selected_e, broken_statement,sleep_time):
    reply=''

    history = "You are a web UI test script repair tool. " \
                   "To repair the broken statement, you chose the element "+selected_e+" as the most similar to the target element from the given candidate element list. "
    instruction2="Now based on your selected element, update the locator and outdated assertion of the broken statement. Give the result of repaired statement."

    user_prompt2="Broken statement: "+broken_statement

    # token_limit=countToken(instruction0+user_prompt1+instruction2+user_prompt2)

    messages3 =  [
        {"role": "system", "content": history+instruction2},
        {"role": "user", "content": user_prompt2},
        {"role": "assistant", "content": ''}
    ]
    while reply=='':
        try:
            start1 = time.time()
            reply=openai.ChatCompletion.create(
                model="gpt-3.5-turbo",
                messages=messages3,
                # max_token=token_limit,
                temperature=0.8
            )
        except openai.error.Timeout as error:
            time.sleep(sleep_time)
            continue
        except openai.error.InvalidRequestError as error:
            print(error)
            print("-----")
            print(messages3)
            print("-----")
            print("This model's maximum context length is 4096 tokens. However, your messages exceeds the limitaion. Please reduce the length of the messages.")
            break
        except openai.error.RateLimitError as error:
            print(f"Rate limit error: {error}")
            print("Waiting for 20 seconds before retrying...")
            if sleep_time<20:
                sleep_time=20
                time.sleep(sleep_time)
            else:
                sleep_time=433
                time.sleep(sleep_time)  # wait for 450 seconds before retrying
        except openai.error.ServiceUnavailableError as error:
            print("openai.error.ServiceUnavailableError: The server is overloaded or not ready yet.")
            time.sleep(sleep_time)

    end1=time.time()
    for choice in reply.choices:
        content = choice.message["content"]
        print(content)

    print("-----")
    print(reply.choices[0].message["content"])
    print("runtime:\r\n"+str(end1-start1)+"\r\n"+"------------------------------------------------")

    return reply.choices[0].message["content"],end1-start1,messages3,sleep_time


if __name__ == "__main__":
    openai.api_key = ""

    to_replace="extract_element"
    # method="xpath" #xpath,water,vista
    methods=["vista","water","xpath"]
    webs=["addressbook","Claroline_Test_Suite","collabtive","mantisbt","mrbs"]
    for method in methods:
        for web in webs:
            # if web!="collabtive":
            #     continue
            path_test = ".../extract_element/" \
                    + method+"/testcases/"+web+"/"
            process_file(path_test,to_replace,method,sleep_time=3)
